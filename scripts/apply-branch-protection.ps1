param(
    [string]$Owner = 'maya27AokiSawada',
    [string]$Repo = 'TaskRingK',
    [string]$Branch = 'main'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'gh command was not found. Install GitHub CLI first.'
}

gh auth status 1>$null
if ($LASTEXITCODE -ne 0) {
    throw 'GitHub CLI is not authenticated. Run gh auth login first.'
}

$payload = @{
    required_status_checks = @{
        strict = $true
        contexts = @(
            'Security Checks / Security Preflight',
            'Security Checks / Gitleaks Scan'
        )
    }
    enforce_admins = $false
    required_pull_request_reviews = @{
        dismiss_stale_reviews = $true
        require_code_owner_reviews = $false
        required_approving_review_count = 1
        require_last_push_approval = $false
    }
    restrictions = $null
    required_conversation_resolution = $true
    required_linear_history = $true
    allow_force_pushes = $false
    allow_deletions = $false
    block_creations = $false
    lock_branch = $false
    allow_fork_syncing = $true
}

$json = $payload | ConvertTo-Json -Depth 8

$apiPath = "repos/$Owner/$Repo/branches/$Branch/protection"
Write-Host "Applying branch protection to $Owner/${Repo}:${Branch} ..."

$response = $json | gh api --method PUT $apiPath --input - --header "Accept: application/vnd.github+json" 2>&1

if ($LASTEXITCODE -ne 0) {
    $errorText = ($response | Out-String)
    if ($errorText -match 'Upgrade to GitHub Pro|HTTP 403') {
        throw 'Branch protection could not be enabled on this private repository plan. Make the repository public first or upgrade the plan, then re-run this script.'
    }
    throw "Failed to apply branch protection via GitHub API.`n$errorText"
}

Write-Host 'Branch protection updated successfully.' -ForegroundColor Green