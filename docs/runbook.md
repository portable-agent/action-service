# Runbook

## Сервис не стартует

1. Проверь `docker compose ps`.
2. Проверь `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.
3. Проверь доступность issuer из `OIDC_ISSUER_URI`.
4. Посмотри `/actuator/health` и журнал Flyway.

## Outbox не уходит

1. Проверь Temporal по адресу `TEMPORAL_TARGET`.
2. Проверь `last_error` и `attempts` в `action_dispatch_outbox`.
3. Не удаляй запись вручную до выяснения причины.

Политика повторов и аварийное восстановление будут дополнены до первого реального коннектора.
