param(
  [string]$BaseUrl = "http://localhost:8080/api/v1"
)

$ErrorActionPreference = "Stop"
$stamp = Get-Date -Format "yyyyMMddHHmmss"

function Invoke-Json($Method, $Uri, $Body = $null, $Token = $null) {
  $headers = @{}
  if ($Token) { $headers["Authorization"] = "Bearer $Token" }
  if ($null -eq $Body) {
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers
  }
  return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers -ContentType "application/json; charset=utf-8" -Body ($Body | ConvertTo-Json -Depth 20)
}

Write-Host "[1/10] Healthcheck"
Invoke-Json GET "$BaseUrl/health" | Out-Null
Invoke-Json GET "$BaseUrl/health/readiness" | Out-Null

Write-Host "[2/10] Création compte owner"
$owner = Invoke-Json POST "$BaseUrl/auth/register" @{
  email = "owner-$stamp@example.com"
  displayName = "Owner Beta"
  password = "motdepassefort"
}

Write-Host "[3/10] Création voyage"
$trip = Invoke-Json POST "$BaseUrl/trips" @{
  name = "Smoke test $stamp"
  startDate = "2026-08-01"
  endDate = "2026-08-15"
  referenceCurrency = "EUR"
  customConstraints = @("Sans porc")
} $owner.accessToken

Write-Host "[4/10] Création participant·es"
$sofia = Invoke-Json POST "$BaseUrl/trips/$($trip.id)/persons" @{
  name = "Sofia"
  livingRest = 2000
  weightMode = "LIVING_REST"
  advancedLivingRest = $false
  vegetarian = $false
  noAlcohol = $false
  livingRestPublic = $true
  customConstraints = @()
  presencePeriods = @(@{ startDate = "2026-08-01"; endDate = "2026-08-15" })
} $owner.accessToken

$karim = Invoke-Json POST "$BaseUrl/trips/$($trip.id)/persons" @{
  name = "Karim"
  livingRest = 0
  weightMode = "AVERAGE"
  advancedLivingRest = $false
  vegetarian = $true
  noAlcohol = $false
  livingRestPublic = $true
  customConstraints = @("Sans porc")
  presencePeriods = @(@{ startDate = "2026-08-01"; endDate = "2026-08-15" })
} $owner.accessToken

Write-Host "[5/10] Création dépenses"
Invoke-Json POST "$BaseUrl/trips/$($trip.id)/expenses" @{
  title = "Courses"
  date = "2026-08-02"
  payerPersonId = $sofia.id
  totalAmount = 120
  meatAmount = 30
  alcoholAmount = 20
  customConstraintAmounts = @{ "Sans porc" = 10 }
  type = "NORMAL"
  advancedMode = $false
  manualParticipantIds = @()
  currency = "EUR"
  exchangeRateToTripCurrency = 1
} $owner.accessToken | Out-Null

Invoke-Json POST "$BaseUrl/trips/$($trip.id)/expenses" @{
  title = "Essence mutualisée"
  date = "2026-08-03"
  payerPersonId = $karim.id
  totalAmount = 250
  meatAmount = 0
  alcoholAmount = 0
  customConstraintAmounts = @{}
  type = "GLOBAL"
  advancedMode = $false
  manualParticipantIds = @()
  currency = "EUR"
  exchangeRateToTripCurrency = 1
} $owner.accessToken | Out-Null

Write-Host "[6/10] Résumé"
$summary = Invoke-Json GET "$BaseUrl/trips/$($trip.id)/summary" $null $owner.accessToken
if ($summary.balances.Count -lt 2) { throw "Résumé invalide : balances insuffisantes" }

Write-Host "[7/10] Invitation et contrôle membre"
$invitation = Invoke-Json POST "$BaseUrl/trips/$($trip.id)/invitations" @{
  roleToGrant = "PARTICIPANT"
  expiresInDays = 7
} $owner.accessToken

$member = Invoke-Json POST "$BaseUrl/auth/register" @{
  email = "member-$stamp@example.com"
  displayName = "Member Beta"
  password = "motdepassefort"
}
Invoke-Json POST "$BaseUrl/trips/$($trip.id)/join" @{
  invitationCode = $invitation.code
} $member.accessToken | Out-Null
Invoke-Json GET "$BaseUrl/trips/$($trip.id)/persons" $null $member.accessToken | Out-Null

Write-Host "[8/10] Exports CSV"
Invoke-WebRequest -Method GET -Uri "$BaseUrl/trips/$($trip.id)/exports/expenses.csv" -Headers @{ Authorization = "Bearer $($owner.accessToken)" } | Out-Null
Invoke-WebRequest -Method GET -Uri "$BaseUrl/trips/$($trip.id)/exports/summary.csv" -Headers @{ Authorization = "Bearer $($owner.accessToken)" } | Out-Null

Write-Host "[9/10] Audit"
$audit = Invoke-Json GET "$BaseUrl/trips/$($trip.id)/audit-logs" $null $owner.accessToken
if ($audit.Count -lt 5) { throw "Audit insuffisant" }

Write-Host "[10/10] Smoke test OK"
Write-Host "Voyage créé : $($trip.id)"
