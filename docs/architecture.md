# Архитектура

```text
controller -> service -> repository -> PostgreSQL
                  |          ^
                  v          |
                model     jOOQ code
                  |
                  v
              scheduler -> workflow -> Temporal
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
- `config` и `exception` — настройка и единый формат ошибок.

Flyway SQL — единственный источник схемы. Gradle создаёт Java-классы jOOQ из тех же SQL-файлов до
компиляции. Поэтому код и схема не расходятся.

OpenAPI из release `portable-agent/contracts` хранится в `src/main/openapi`. Gradle создаёт интерфейс
`ActionsApi` и HTTP-модели в `build/generated-src/openapi`. Generated-код не коммитится. Контроллер
реализует этот интерфейс, а service и domain не зависят от HTTP-классов.

## Текущий продуктовый срез

`ActionService` разрешает только `calendar.create_event` через `fake-calendar`. Проверка находится в
service-слое до вычисления hash и сохранения нового действия. Повтор с существующим `requestKey`
по-прежнему возвращает ранее сохранённое действие. Конкурентные повторы защищены ограничением БД и
jOOQ `ON CONFLICT DO NOTHING`; запись outbox создаёт только запрос, который сохранил действие.
Точный payload хранится в PostgreSQL как `JSONB` и возвращается вместе с `payloadHash`. Поэтому виджет
и будущий workflow используют данные сохранённого действия, а не новый ответ AI.

`V2` очищает только записи pre-MVP-схемы `V1` перед добавлением обязательного payload. Это явный
одноразовый reset: старый каркас сохранял только hash, из которого нельзя восстановить исходные данные.

## Пока не решено

- как выполняются платежи, встречи и задачи;
- политика повторов и лимиты;
- формат результата внешнего коннектора;
- полный жизненный цикл Temporal workflow.
