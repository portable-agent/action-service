# Архитектура

```text
controller -> service -> repository -> PostgreSQL
                  |          ^
                  v          |
                model     jOOQ code

controller -> ActionService -> PostgreSQL + outbox
scheduler  -> TemporalSender -> Temporal workflow
Temporal worker -> ActionActivity -> ActionService
                               \-> McpClient -> MCP Gateway -> Calendar MCP
```

## Папки

- `controller` — HTTP без бизнес-логики.
- `api` — generated интерфейс и HTTP-модели из закреплённого OpenAPI.
- `controller` — HTTP-adapter между generated API и командами service-слоя.
- `service` — сценарии, проверки и транзакции.
- `repository` — типобезопасные запросы jOOQ.
- `model` — простые Java-классы предметной области.
- `scheduler` — чтение и отправка outbox.
- `workflow` — клиент Temporal.
- `client` — HTTP-вызов MCP Gateway и OAuth2 service token.
- `config` и `exception` — настройка и единый формат ошибок.

Flyway SQL — единственный источник схемы. Gradle создаёт Java-классы jOOQ из тех же SQL-файлов до
компиляции. Поэтому код и схема не расходятся.

OpenAPI из одного release `portable-agent/contracts` хранится в `src/main/openapi`. Gradle создаёт
интерфейс `ActionsApi` и его HTTP-модели в `build/generated-src/openapi`, а сетевые модели MCP Gateway —
в `build/generated-src/mcp-openapi`. Generated-код не коммитится. Контроллер реализует generated
интерфейс, а MCP-клиент отправляет и читает generated DTO. Service и domain от HTTP-классов не зависят.

## Текущий продуктовый срез

`ActionService` разрешает только `calendar.create_event` через `fake-calendar`. Проверка находится в
service-слое до вычисления hash и сохранения нового действия. Повтор с существующим `requestKey`
по-прежнему возвращает ранее сохранённое действие. Конкурентные повторы защищены ограничением БД и
jOOQ `ON CONFLICT DO NOTHING`; запись outbox создаёт только запрос, который сохранил действие.
Точный payload хранится в PostgreSQL как `JSONB` и возвращается вместе с `payloadHash`. Поэтому виджет
и будущий workflow используют данные сохранённого действия, а не новый ответ AI.

`V2` очищает только записи pre-MVP-схемы `V1` перед добавлением обязательного payload. Это явный
одноразовый reset: старый каркас сохранял только hash, из которого нельзя восстановить исходные данные.

## Жизненный цикл действия

```text
AWAITING_APPROVAL -> APPROVED -> EXECUTING -> SUCCEEDED
                 \-> CANCELLED             \-> FAILED
```

Решение пользователя проверяет сохранённый `payloadHash`. Точный повтор `CONFIRM` или `CANCEL` ничего
не меняет. Внутренние команды worker `start`, `succeed` и `fail` работают по глобальному `actionId` и
также безопасны при повторе. Это нужно, потому что Temporal может повторно выполнить activity после
сетевой ошибки. Поздний повтор `CONFIRM` разрешён и после начала либо завершения выполнения. При
конкурентном изменении сервис перечитывает запись и проверяет переход снова. Успешное действие принимает
повтор только с тем же `eventId`.

## Выполнение через Temporal

Outbox запускает workflow с постоянным id `action-{actionId}`. Workflow ждёт сигнал решения. Для
`CANCEL` он завершается без activity. Для `CONFIRM` он передаёт `actionId` и подтверждённый
`payloadHash` в activity.

`ActionService` сохраняет смену статуса и событие решения в одной транзакции. Scheduler читает это
событие и вызывает Temporal `signalWithStart`. Поэтому сбой процесса между PostgreSQL и Temporal не
теряет решение: неотправленная запись остаётся в outbox. Для одного действия хранится не больше одного
события каждого типа, поэтому повтор того же решения безопасен. Реплики scheduler забирают строки через
`FOR UPDATE SKIP LOCKED`; после ошибки запись получает растущую паузу до следующей попытки и не блокирует
новые события.

Activity повторно сверяет hash, переводит действие в `EXECUTING`, вызывает только настроенный адрес
MCP Gateway и сохраняет `eventId`. Temporal делает не больше трёх попыток. Если все они завершились
ошибкой, отдельная activity переводит действие в `FAILED`. HTTP-ответы и детали ошибок внешнего
коннектора не сохраняются и не отдаются пользователю.

Gateway и OAuth2 включаются только через `MCP_GATEWAY_ENABLED=true`. Все адреса, client id, secret,
scopes и tenant приходят из окружения. Для первого среза один worker работает только с одним tenant и
проверяет его до получения токена. Вызовы token endpoint и Gateway имеют явные connect/read timeouts.
По умолчанию worker не создаётся, поэтому API можно разрабатывать без запущенных Gateway и Keycloak.

## Пока не решено

- как выполняются платежи, встречи и задачи;
- разные правила повторов для временных и постоянных ошибок;
- безопасный multi-tenant token exchange для production Keycloak.
