# API

Точный публичный контракт хранится в репозитории `portable-agent/contracts`. Версионный снимок лежит в
`src/main/openapi`, а Java interface и models создаются в `build/`.

Технический каркас содержит три маршрута:

- `POST /api/v1/actions` — создать действие;
- `GET /api/v1/actions/{actionId}` — получить действие своего tenant;
- `POST /api/v1/actions/{actionId}/decisions` — передать решение.

В первом продуктовом срезе создание принимает только `calendar.create_event` с коннектором
`fake-calendar`. Ответ содержит сохранённый `payload` и его `payloadHash`. Повтор с тем же `requestKey`
возвращает ранее созданное действие вместе с тем же payload.

Точный повтор решения с тем же `payloadHash` возвращает текущее действие без новой записи в БД.
Попытка изменить уже принятое решение или подменить hash возвращает конфликт.

Сервис использует контракт `portable-agent/contracts v2.0.0`. После успешного выполнения ответ содержит
`result.eventId`. До состояния `SUCCEEDED` поле `result` отсутствует.

Внутренний `POST /api/v1/calls` MCP Gateway также берётся из этого release. Его request/response DTO
создаёт OpenAPI Generator; вручную в Action Service они не описываются.

JWT должен содержать `sub` с id пользователя и `tenant_id` с id пространства. Семантика полей и
правила доступа будут уточнены вместе с бизнес-сценариями.
