# Action Service

Сервис хранит действия Portable Agent и безопасно выполняет подтверждённые действия. Он принимает
действие, сохраняет payload вместе с записью outbox и передаёт идентификатор в Temporal. Worker ждёт
решение пользователя и после `CONFIRM` вызывает MCP Gateway. В первом продуктовом срезе разрешено
только `calendar.create_event` через `fake-calendar`.

## Стек

Java 25, Spring Boot 4.1, Spring MVC, jOOQ, PostgreSQL, Flyway, Temporal Java SDK,
OAuth2 Resource Server, Micrometer и Testcontainers. JPA и Hibernate не используются.
Java-код форматируется Palantir Java Format с отступом в четыре пробела, без символов tab.

## Запуск

```bash
docker compose up -d
./gradlew bootRun
```

По умолчанию API ожидает JWT от Keycloak. Для локальной разработки issuer задаётся переменной
`OIDC_ISSUER_URI`. Контракт API находится в репозитории `portable-agent/contracts`.

Worker MCP выключен по умолчанию. Для его запуска нужны настройки без значений, зашитых в image:

```text
MCP_GATEWAY_ENABLED=true
MCP_GATEWAY_URL=http://mcp-gateway:8080
MCP_GATEWAY_TOKEN_URL=http://keycloak:8080/realms/portable-agent/protocol/openid-connect/token
MCP_GATEWAY_CLIENT_ID=action-service
MCP_GATEWAY_CLIENT_SECRET=<secret>
MCP_GATEWAY_SCOPE=mcp:call calendar:write
MCP_GATEWAY_TENANT_ID=<tenant UUID>
MCP_GATEWAY_CONNECT_TIMEOUT=3s
MCP_GATEWAY_READ_TIMEOUT=10s
```

Токен получается стандартным OAuth2 `client_credentials` и кешируется до безопасного времени
обновления. Один worker обслуживает только указанный tenant; действие другого tenant отклоняется до
сетевого вызова. Секрет не хранится в Git и не входит в Docker image.

## Проверки

```bash
./gradlew spotlessCheck test
```

Код jOOQ создаётся автоматически из `src/main/resources/db/migration/*.sql`. Сгенерированный код
находится в `build/` и не хранится в Git.

HTTP-интерфейс Action API и сетевые модели MCP Gateway создаёт OpenAPI Generator из двух закреплённых
снимков в `src/main/openapi`. Оба снимка обновляются вместе только из одного GitHub Release репозитория
contracts:

```powershell
pwsh ./scripts/update-contract.ps1 -Version 1.2.0
```

Миграция `V2` один раз удаляет тестовые записи старого pre-MVP-каркаса: в схеме `V1` payload не
хранился, поэтому восстановить его из одного hash невозможно. Production-данных у этой версии нет.
Миграция `V3` добавляет nullable JSONB-поле `result`. Оно заполняется только после успешного выполнения.
Миграция `V4` делает outbox общим для запуска workflow и передачи решения пользователя. Изменение
статуса и запись события происходят в одной транзакции PostgreSQL.

Подтверждение и отмена безопасны при повторе того же запроса. Внутренние операции worker
`start`, `succeed` и `fail` также идемпотентны: повтор не меняет версию и время действия. Другой
`eventId` после успеха отклоняется как конфликт.

Action API и outbound-вызов MCP Gateway соответствуют bundle `portable-agent/contracts` версии `1.2.0`.

## Где читать дальше

- `AGENTS.md` — короткая памятка о границах сервиса и командах.
- `docs/architecture.md` — структура кода и зависимости слоёв.
- `docs/development.md` — локальная разработка и TDD.
- `docs/runbook.md` — запуск и диагностика.
