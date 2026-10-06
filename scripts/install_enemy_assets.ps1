param(
    [string]$Package = "$env:USERPROFILE\Downloads\neonmarshal_enemy_assets_ready.zip"
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$target = Join-Path $repo "app\src\main\assets\sprites\enemies"
$required = @(
    "heavy_elite_atlas.webp",
    "sewer_mutant_atlas.webp",
    "stalker_beast_atlas.webp",
    "hooded_operator_atlas.webp",
    "atlas.json"
)

if (-not (Test-Path -LiteralPath $Package)) {
    throw ("Enemy asset package not found: " + $Package + ". Download neonmarshal_enemy_assets_ready.zip and run this script again.")
}

New-Item -ItemType Directory -Force -Path $target | Out-Null
$tmp = Join-Path $env:TEMP ("neonmarshal-enemy-assets-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

try {
    Expand-Archive -LiteralPath $Package -DestinationPath $tmp -Force
    foreach ($file in $required) {
        $source = Join-Path $tmp $file
        if (-not (Test-Path -LiteralPath $source)) {
            throw ("Package entry missing: " + $file)
        }
        Copy-Item -LiteralPath $source -Destination (Join-Path $target $file) -Force
    }

    Write-Host "Enemy assets installed to:"
    Write-Host ("  " + $target)
    Write-Host ""
    Get-ChildItem -LiteralPath $target -File |
        Where-Object { $_.Name -in $required } |
        Select-Object Name, Length
}
finally {
    Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
}