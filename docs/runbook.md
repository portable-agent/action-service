# Runbook

## Сервис не стартует

1. Проверь `docker compose ps`.
2. Проверь `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.
3. Проверь доступность issuer из `OIDC_ISSUER_URI`.
4. Посмотри `/actuator/health` и журнал Flyway.

## Worker не выполняет действие

1. Проверь `MCP_GATEWAY_ENABLED`: без `true` worker намеренно не создаётся.
2. Проверь `TEMPORAL_TARGET`, `TEMPORAL_NAMESPACE` и `TEMPORAL_TASK_QUEUE`.
3. Проверь `MCP_GATEWAY_URL` и доступность `/health` Gateway.
4. Проверь token URL, client id, secret, scopes `mcp:call calendar:write` и `MCP_GATEWAY_TENANT_ID`.
5. Проверь, что connect/read timeouts меньше 30-секундного timeout activity.
6. Не выводи access token или client secret в журнал и issue.
7. Найди workflow `action-{actionId}` в Temporal UI и сравни статус action в PostgreSQL.

## Outbox не уходит

1. Проверь Temporal по адресу `TEMPORAL_TARGET`.
2. Проверь `last_error` и `attempts` в `action_dispatch_outbox`.
3. Не удаляй запись вручную до выяснения причины.

Scheduler повторно берёт неотправленные записи. Activity выполняется не больше трёх раз, после чего
действие получает статус `FAILED`. Outbox увеличивает паузу между неудачными отправками до пяти минут;
несколько реплик делят записи через `SKIP LOCKED`. Разделение временных и постоянных ошибок будет
добавлено перед первым реальным коннектором.
