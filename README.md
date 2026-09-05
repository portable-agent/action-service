# Action Service

Сервис хранит действия Portable Agent и готовит их к безопасному выполнению. Он принимает действие,
сохраняет его payload вместе с записью outbox и передаёт идентификатор в Temporal. В первом продуктовом
срезе разрешено только `calendar.create_event` через `fake-calendar`. Настоящие интеграции появятся
позже.

## Стек

Java 25, Spring Boot 4.1, Spring MVC, jOOQ, PostgreSQL, Flyway, Temporal Java SDK,
OAuth2 Resource Server, Micrometer и Testcontainers. JPA и Hibernate не используются.

## Запуск

```bash
docker compose up -d
./gradlew bootRun
```

По умолчанию API ожидает JWT от Keycloak. Для локальной разработки issuer задаётся переменной
`OIDC_ISSUER_URI`. Контракт API находится в репозитории `portable-agent/contracts`.

## Проверки

```bash
./gradlew spotlessCheck test
```

Код jOOQ создаётся автоматически из `src/main/resources/db/migration/*.sql`. Сгенерированный код
находится в `build/` и не хранится в Git.

HTTP-интерфейс и API-модели создаёт OpenAPI Generator из закреплённого снимка
`src/main/openapi/action-api.yaml`. Снимок обновляется только из GitHub Release репозитория contracts:

```powershell
pwsh ./scripts/update-contract.ps1 -Version 1.1.0
```

Миграция `V2` один раз удаляет тестовые записи старого pre-MVP-каркаса: в схеме `V1` payload не
хранился, поэтому восстановить его из одного hash невозможно. Production-данных у этой версии нет.
Миграция `V3` добавляет nullable JSONB-поле `result`. Оно заполняется только после успешного выполнения.

## Где читать дальше

- `AGENTS.md` — короткая памятка о границах сервиса и командах.
- `docs/architecture.md` — структура кода и зависимости слоёв.
- `docs/development.md` — локальная разработка и TDD.
- `docs/runbook.md` — запуск и диагностика.
