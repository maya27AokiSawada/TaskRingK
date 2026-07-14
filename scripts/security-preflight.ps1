Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message =="
}

function Fail {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
    return $true
}

function Pass {
    param([string]$Message)
    Write-Host "[OK]   $Message" -ForegroundColor Green
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git command was not found."
}

$failures = 0

Write-Section "Checking tracked sensitive file paths"
$forbiddenTrackedPaths = @(
    '.env',
    '.env.local',
    'app/google-services.json',
    'google-services.json',
    'firebase_options.kt',
    'key.properties'
)

$tracked = git ls-files
foreach ($path in $forbiddenTrackedPaths) {
    if ($tracked -contains $path) {
        if (Fail "Tracked sensitive file detected: $path") { $failures++ }
    }
}

$forbiddenExtensions = @('*.jks', '*.keystore', '*.p12', '*.pem')
foreach ($pattern in $forbiddenExtensions) {
    $hit = git ls-files $pattern
    if ($hit) {
        if (Fail "Tracked sensitive artifact detected by pattern $pattern`n$($hit -join "`n")") { $failures++ }
    }
}

if ($failures -eq 0) {
    Pass "No forbidden sensitive files are tracked."
}

Write-Section "Scanning HEAD tracked files for high-risk secret patterns"
$contentPatterns = @{
    'Google API key (AIza...)' = 'AIza[0-9A-Za-z_-]{20,}'
    'GitHub token (ghp_)' = 'ghp_[A-Za-z0-9]{30,}'
    'AWS key id (AKIA...)' = 'AKIA[0-9A-Z]{16}'
    'Private key header' = 'BEGIN (RSA|EC|OPENSSH|PRIVATE) KEY'
    'Client secret assignment' = 'client_secret\s*[:=]\s*["''][^"'']+'
    'Access token assignment' = 'access_token\s*[:=]\s*["''][^"'']+'
    'Refresh token assignment' = 'refresh_token\s*[:=]\s*["''][^"'']+'
}

foreach ($name in $contentPatterns.Keys) {
    $pattern = $contentPatterns[$name]
    $headHit = git grep -n -I -E $pattern -- .
    if ($LASTEXITCODE -gt 1) {
        if (Fail "git grep failed while checking pattern '$name'.") { $failures++ }
        continue
    }
    if ($LASTEXITCODE -eq 0 -and $headHit) {
        if (Fail "$name detected in tracked files:`n$headHit") { $failures++ }
    }
}

if ($failures -eq 0) {
    Pass "No high-risk patterns were found in tracked HEAD files."
}

Write-Section "Scanning full git history for high-risk secret traces"
$historyPatterns = @{
    'Google API key trace in history' = 'AIza[0-9A-Za-z_-]{20,}'
    'GitHub token trace in history' = 'ghp_[A-Za-z0-9]{30,}'
    'AWS key id trace in history' = 'AKIA[0-9A-Z]{16}'
    'Private key trace in history' = 'BEGIN (RSA|EC|OPENSSH|PRIVATE) KEY'
}

foreach ($name in $historyPatterns.Keys) {
    $pattern = $historyPatterns[$name]
    $historyHit = git log --all -G $pattern --pretty=format:'%h %ad %an %s' --date=short -- .
    if ($LASTEXITCODE -ne 0) {
        if (Fail "git log failed while checking history pattern '$name'.") { $failures++ }
        continue
    }
    if ($LASTEXITCODE -eq 0 -and $historyHit) {
        if (Fail "$name found in history commits:`n$historyHit") { $failures++ }
    }
}

if ($failures -eq 0) {
    Pass "No high-risk traces were found in git history."
}

Write-Section "Final result"
if ($failures -gt 0) {
    Write-Host "Security preflight failed with $failures issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host 'Security preflight passed.' -ForegroundColor Green
exit 0