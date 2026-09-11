param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$repoPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$targetDirectory = [System.IO.Path]::GetFullPath((Join-Path $repoPath 'src/main/openapi'))
$fileId = [guid]::NewGuid()
$contracts = @(
    @{ Name = 'action-api'; Target = Join-Path $targetDirectory 'action-api.yaml' },
    @{ Name = 'mcp-gateway-api'; Target = Join-Path $targetDirectory 'mcp-gateway-api.yaml' }
)
foreach ($contract in $contracts) {
    $contract.Stage = Join-Path $targetDirectory ".$($contract.Name)-$fileId.yaml"
    $contract.Backup = Join-Path $targetDirectory ".$($contract.Name)-$fileId.backup"
}
$tempPath = Join-Path ([System.IO.Path]::GetTempPath()) ("portable-agent-contracts-" + [guid]::NewGuid())
$archiveName = "portable-agent-contracts-$Version.tgz"
$archivePath = Join-Path $tempPath $archiveName
$checksumPath = Join-Path $tempPath 'SHA256SUMS'
$releaseUrl = "https://github.com/portable-agent/contracts/releases/download/v$Version"
$replaced = @()

try {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw 'GitHub CLI is required to verify the contract attestation.'
    }
    New-Item -ItemType Directory -Path $tempPath | Out-Null
    Invoke-WebRequest -Uri "$releaseUrl/$archiveName" -OutFile $archivePath
    Invoke-WebRequest -Uri "$releaseUrl/SHA256SUMS" -OutFile $checksumPath

    $escapedArchiveName = [regex]::Escape($archiveName)
    $checksumLines = @(Get-Content -LiteralPath $checksumPath | Where-Object {
        $_ -match "^(?<hash>[a-fA-F0-9]{64})\s+\*?$escapedArchiveName$"
    })
    if ($checksumLines.Count -ne 1) {
        throw 'Checksum file does not contain exactly one entry for the contract bundle.'
    }
    $null = $checksumLines[0] -match '^(?<hash>[a-fA-F0-9]{64})'
    $expectedHash = $Matches.hash.ToUpperInvariant()
    $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $archivePath).Hash
    if ($actualHash -ne $expectedHash) {
        throw 'Checksum contract bundle does not match the release.'
    }

    & gh attestation verify $archivePath --repo portable-agent/contracts
    if ($LASTEXITCODE -ne 0) {
        throw 'Cannot verify the GitHub attestation for the contract bundle.'
    }

    $archiveFiles = $contracts | ForEach-Object { "package/openapi/$($_.Name).yaml" }
    & tar -xzf $archivePath -C $tempPath $archiveFiles
    if ($LASTEXITCODE -ne 0) {
        throw 'Cannot unpack contract bundle.'
    }

    foreach ($contract in $contracts) {
        $sourcePath = Join-Path $tempPath "package/openapi/$($contract.Name).yaml"
        $sourceText = Get-Content -Raw -LiteralPath $sourcePath
        if ($sourceText -notmatch "(?m)^  version: $([regex]::Escape($Version))$") {
            throw "$($contract.Name) version does not match the requested release."
        }
        Copy-Item -LiteralPath $sourcePath -Destination $contract.Stage
    }

    try {
        foreach ($contract in $contracts) {
            [System.IO.File]::Replace($contract.Stage, $contract.Target, $contract.Backup, $true)
            $replaced += $contract
        }
    } catch {
        foreach ($contract in $replaced) {
            Copy-Item -LiteralPath $contract.Backup -Destination $contract.Target -Force
        }
        throw
    }

    Write-Output "Action API and MCP Gateway API updated to version $Version."
} finally {
    foreach ($contract in $contracts) {
        foreach ($localPath in @($contract.Stage, $contract.Backup)) {
            if (Test-Path -LiteralPath $localPath) {
                Remove-Item -LiteralPath $localPath -Force
            }
        }
    }
    $resolvedTempPath = [System.IO.Path]::GetFullPath($tempPath)
    $systemTempPath = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if ($resolvedTempPath.StartsWith($systemTempPath, [System.StringComparison]::OrdinalIgnoreCase) -and
        (Test-Path -LiteralPath $resolvedTempPath)) {
        Remove-Item -LiteralPath $resolvedTempPath -Recurse -Force
    }
}
