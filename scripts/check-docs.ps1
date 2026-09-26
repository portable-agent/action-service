$ErrorActionPreference = "Stop"

$requiredFiles = @(
    "README.md",
    "AGENTS.md",
    "catalog-info.yaml",
    "mkdocs.yml",
    "docs/index.md",
    "docs/api.md",
    "docs/architecture.md",
    "docs/development.md",
    "docs/runbook.md",
    "docs/decisions/0001-jooq.md",
    "src/main/openapi/action-api.yaml",
    "src/main/openapi/mcp-gateway-api.yaml"
)

$missingFiles = $requiredFiles | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) }
if ($missingFiles.Count -gt 0) {
    throw "Required files are missing: $($missingFiles -join ', ')"
}

$catalogText = Get-Content -LiteralPath "catalog-info.yaml" -Raw
if ($catalogText -notmatch "backstage\.io/techdocs-ref:\s*dir:\.") {
    throw "catalog-info.yaml has no backstage.io/techdocs-ref: dir:."
}

$mkdocsText = Get-Content -LiteralPath "mkdocs.yml" -Raw
if ($mkdocsText -notmatch "(?m)^docs_dir:\s*docs\s*$") {
    throw "mkdocs.yml must contain docs_dir: docs."
}

$contractVersions = @(
    "src/main/openapi/action-api.yaml",
    "src/main/openapi/mcp-gateway-api.yaml"
) | ForEach-Object {
    $contractText = Get-Content -LiteralPath $_ -Raw
    if ($contractText -notmatch "(?m)^  version: (?<version>\d+\.\d+\.\d+)$") {
        throw "$_ has no OpenAPI version."
    }
    $Matches.version
}
if (($contractVersions | Select-Object -Unique).Count -ne 1) {
    throw "Action API and MCP Gateway API use different contract versions."
}

Write-Host "Action Service documentation follows the project standard."
