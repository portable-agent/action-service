# Action Service

Сервис хранит действия Portable Agent и готовит их к безопасному выполнению. Сейчас это каркас:
он принимает действие, сохраняет его вместе с записью outbox и передаёт идентификатор в Temporal.
Реальные интеграции и правила выполнения появятся после общего проектирования бизнеса.

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

## Где читать дальше

- `AGENTS.md` — короткая памятка о границах сервиса и командах.
- `docs/architecture.md` — структура кода и зависимости слоёв.
- `docs/development.md` — локальная разработка и TDD.
- `docs/runbook.md` — запуск и диагностика.
