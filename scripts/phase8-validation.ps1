Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-CheckedCommand {
    param(
        [string]$Command,
        [string]$Label
    )

    Write-Host "[RUN] $Label"
    Invoke-Expression $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed ($Label). Exit code: $LASTEXITCODE"
    }
}

Write-Host "== Phase 8 validation: automated checks =="
Write-Host "Prerequisite: use JDK 21 for Gradle/Kotlin tasks in this repository."

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "[WARN] adb was not found. Two-device test requires Android SDK platform-tools." -ForegroundColor Yellow
}

Invoke-CheckedCommand "./gradlew :app:testDevDebugUnitTest --tests '*WhiteboardViewModelTest'" "WhiteboardViewModel tests"
Invoke-CheckedCommand "./gradlew :app:testDevDebugUnitTest --tests '*HybridWhiteboardRepositoryImplTest'" "HybridWhiteboardRepositoryImpl tests"

Write-Host ""
Write-Host "== Phase 8 validation: two-device readiness =="

if (Get-Command adb -ErrorAction SilentlyContinue) {
    $deviceLines = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" }
    $deviceCount = @($deviceLines).Count
    if ($deviceCount -ge 2) {
        Write-Host "[OK] $deviceCount devices/emulators detected."
    } else {
        Write-Host "[WARN] $deviceCount device detected. Start at least 2 devices for realtime sync validation." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Manual checklist: docs/testing/phase8_validation.md"
Write-Host "Phase 8 validation automation finished." -ForegroundColor Green