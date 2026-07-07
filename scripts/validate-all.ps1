$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

function Run-Step($Label, $Command) {
    Write-Host "==> $Label"
    Push-Location $Root
    try {
        cmd /c $Command
        if ($LASTEXITCODE -ne 0) { throw "Échec: $Label" }
    } finally {
        Pop-Location
    }
}

Run-Step "Backend — mvnw.cmd clean test" "cd backend-api && mvnw.cmd clean test"
Run-Step "Frontend — run-tests.bat" "cd frontend-web && run-tests.bat"
Run-Step "Android — gradlew.bat testDebugUnitTest assembleDebug" "cd mobile-android && gradlew.bat testDebugUnitTest assembleDebug"

Write-Host "Validation complète OK."
