# Снимки API

`action-api.yaml` и `mcp-gateway-api.yaml` взяты из одного release `portable-agent/contracts`. Точная
версия записана в `info.version` самих файлов, поэтому отдельная копия номера версии не хранится.

Файл не редактируют вручную. Новая версия сначала выходит в репозитории contracts, затем снимок
обновляется командой:

```powershell
pwsh ./scripts/update-contract.ps1 -Version X.Y.Z
```

Скрипт скачивает release bundle, находит checksum именно этого архива, проверяет GitHub artifact
attestation, извлекает оба OpenAPI и проверяет их версии. Оба файла сначала готовятся рядом с целевыми,
а при ошибке замены предыдущий файл восстанавливается из backup. Для запуска нужен авторизованный
GitHub CLI (`gh`). Обновление снимков отправляется отдельным pull request. Генерируемый Java-код
находится в `build/` и не хранится в Git.
