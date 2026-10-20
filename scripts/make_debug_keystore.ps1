# Generates the repo-local debug keystore expected by app/build.gradle.kts.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\make_debug_keystore.ps1
#
# The keystore is gitignored on purpose (see .gitignore). Clean checkouts still
# build: app/build.gradle.kts falls back to the standard AGP debug signing
# config when this file is absent.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $repoRoot "debug.keystore"

if (Test-Path $keystore) {
    Write-Host "debug.keystore already exists at $keystore"
    exit 0
}

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    $javaHome = $env:JAVA_HOME
    if ($javaHome) {
        $candidate = Join-Path $javaHome "bin\keytool.exe"
        if (Test-Path $candidate) { $keytool = $candidate }
    }
}
if (-not $keytool) {
    Write-Error "keytool not found. Install a JDK and/or set JAVA_HOME."
}

& $keytool -genkeypair -v `
    -keystore $keystore `
    -storepass android -keypass android `
    -alias androiddebugkey `
    -keyalg RSA -keysize 2048 -validity 10950 `
    -dname "CN=Android Debug,O=Android,C=US"

if ($LASTEXITCODE -ne 0) { Write-Error "keytool failed with exit code $LASTEXITCODE" }
Write-Host "Created $keystore"
