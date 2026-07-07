$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Fail($message) {
    Write-Error "[SECURITY] $message"
    exit 1
}

Write-Host "[SECURITY] Vérification des fichiers d'environnement sensibles..."
$envFiles = git ls-files | Select-String -Pattern '(^|/)\.env(\..*)?$' | ForEach-Object { $_.Line } | Where-Object { $_ -notmatch '(^|/)\.env(\..*)?\.example$' }
if ($envFiles) {
    $envFiles | ForEach-Object { Write-Host $_ }
    Fail "Un fichier .env réel est suivi par Git. Seuls les fichiers *.example doivent être versionnés."
}

Write-Host "[SECURITY] Vérification des artefacts locaux suivis par Git..."
$artifacts = git ls-files | Select-String -Pattern '(^|/)(target|build|\.gradle|node_modules)/' | ForEach-Object { $_.Line }
if ($artifacts) {
    $artifacts | ForEach-Object { Write-Host $_ }
    Fail "Des artefacts locaux sont suivis par Git. Nettoyer l'index avant release."
}

Write-Host "[SECURITY] OK"
