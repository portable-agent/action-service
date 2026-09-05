# Снимок Action API

`action-api.yaml` взят из release `portable-agent/contracts`. Точная версия записана в `info.version`
самого файла, поэтому отдельная копия номера версии не хранится.

Файл не редактируют вручную. Новая версия сначала выходит в репозитории contracts, затем снимок
обновляется командой:

```powershell
pwsh ./scripts/update-contract.ps1 -Version X.Y.Z
```

Скрипт скачивает release bundle, находит checksum именно этого архива, проверяет GitHub artifact
attestation, извлекает только OpenAPI и проверяет его версию. Файл заменяется через staged-копию в той
же папке. Для запуска нужен авторизованный GitHub CLI (`gh`). Обновление снимка отправляется отдельным pull request.
Генерируемый Java-код находится в `build/` и не хранится в Git.
