param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectId
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    throw 'firebase command was not found. Install Firebase CLI first.'
}

Write-Host "Deploying Firestore rules to project: $ProjectId"
firebase deploy --only firestore --project $ProjectId

if ($LASTEXITCODE -ne 0) {
    throw 'Failed to deploy Firestore rules.'
}

Write-Host 'Firestore rules deployed successfully.' -ForegroundColor Green