# Разработка

## TDD

Работа идёт коротким циклом: красный тест, простой рабочий код, рефакторинг. Основная масса тестов —
быстрые unit-тесты без Spring. HTTP проверяется MVC-тестами, SQL — через настоящий PostgreSQL в
Testcontainers.

## Команды

```bash
docker compose up -d
./gradlew jooqCodegen
./gradlew spotlessApply test
./gradlew bootRun
```

После новой Flyway-миграции запусти `jooqCodegen`. Редактировать файлы в `build/generated-src`
нельзя: Gradle перезапишет их.
