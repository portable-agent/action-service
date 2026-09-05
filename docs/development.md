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

Generated API не меняется вручную. Для обновления закреплённого контракта используй
`pwsh ./scripts/update-contract.ps1 -Version X.Y.Z`, затем запусти тесты и проверь adapter в
`controller`.
