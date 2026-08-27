# Action Service

Транзакционное ядро действий Portable Agent. Сервис принимает предложение действия, вычисляет
неизменяемый hash payload, сохраняет action и outbox в одной транзакции и запускает durable workflow
в Temporal. Подтверждение действительно только для того payload, который увидел пользователь.

## Стек

Java 25, Spring Boot 4.1, PostgreSQL, Flyway, Temporal Java SDK, OAuth2 Resource Server,
OpenTelemetry-ready Actuator/Micrometer, Testcontainers.

## Запуск

```bash
docker compose up -d
./gradlew bootRun
```

По умолчанию API ожидает JWT от Keycloak. Для локальной разработки issuer задаётся переменной
`OIDC_ISSUER_URI`. Контракт API находится в репозитории `portable-agent/contracts`.

## Архитектурная гарантия

HTTP-слой не работает с JPA напрямую. Бизнес-переходы принадлежат aggregate `ActionProposal`,
транзакции — application service, доставка в Temporal — outbox dispatcher. Повторная доставка
безопасна благодаря стабильному workflow id `action-{uuid}`.
