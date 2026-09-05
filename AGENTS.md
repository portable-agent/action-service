# Памятка по Action Service

## Ответственность

Сервис хранит состояние, payload и результат действия, защищает повторные запросы ключом `requestKey`, пишет outbox в
той же транзакции и запускает workflow Temporal. Сервис пока является каркасом: не придумывай
правила оплаты, календаря, Jira или подтверждения без решения команды.

## Границы

- `controller` принимает HTTP и вызывает один метод `service`.
- `service` содержит сценарии и транзакции.
- `repository` содержит только запросы jOOQ.
- `model` не зависит от Spring, jOOQ и HTTP.
- `api` создаётся OpenAPI Generator в `build/`; generated-файлы не меняются вручную.
- `controller` переводит generated API models в простые команды service-слоя.
- `scheduler` отправляет outbox.
- `workflow` содержит связь с Temporal.
- Схема БД меняется только Flyway-миграциями.
- JPA и Hibernate запрещены.

## Простые имена

Используй английские слова уровня A2–B1: `create`, `get`, `save`, `send`, `find`, `status`, `error`.
Если нужен редкий технический термин (`outbox`, `workflow`, `idempotency`), объясни его в docs.

## Рабочий цикл

1. Сначала тест с понятным именем `method_whenCase_shouldResult`.
2. Затем самый простой код, который делает тест зелёным.
3. После зелёного теста — рефакторинг.
4. Перед завершением: `./gradlew spotlessApply test`.

## Важные команды

```bash
./gradlew jooqCodegen
./gradlew openApiGenerate
./gradlew spotlessApply test
./gradlew bootRun
```
