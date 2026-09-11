# Разработка

## TDD

Работа идёт коротким циклом: красный тест, простой рабочий код, рефакторинг. Основная масса тестов —
быстрые unit-тесты без Spring. HTTP проверяется MVC-тестами, SQL — через настоящий PostgreSQL в
Testcontainers.

## Команды

```bash
docker compose up -d
./gradlew jooqCodegen
./gradlew openApiGenerate
./gradlew spotlessApply test
./gradlew bootRun
```

После новой Flyway-миграции запусти `jooqCodegen`. Редактировать файлы в `build/generated-src`
нельзя: Gradle перезапишет их.

Generated API не меняется вручную. Для обновления обоих закреплённых контрактов используй
`pwsh ./scripts/update-contract.ps1 -Version X.Y.Z`, затем запусти тесты и проверь adapters в
`controller` и `client`.

Для unit-тестов Temporal используется in-memory `TestWorkflowEnvironment`. HTTP к MCP Gateway и
token endpoint проверяется через `MockRestServiceServer`, поэтому тесты не требуют сети и секретов.
Изоляция первого среза намеренно простая: один запущенный worker обслуживает один
`MCP_GATEWAY_TENANT_ID`. Multi-tenant token exchange проектируется до подключения реального календаря.
Полный путь с настоящими контейнерами проверяется в `portable-agent/test-lab` после обновления
репозитория `deploy`.
