# =============================================================================
# 4. CREATE SYNC VERIFICATION SCRIPT
# File: verify-sync.ps1

Write-Host "`n🔄 DOCUMENTATION SYNC VERIFICATION" -ForegroundColor Cyan

# Check if documentation claims match reality
Write-Host "`n📋 Checking documentation accuracy..." -ForegroundColor Yellow

$syncIssues = @()

# Check if any docs mention non-existent files
$docFiles = Get-ChildItem -Name "*.md" -Recurse
foreach ($doc in $docFiles) {
    $content = Get-Content $doc -Raw
    
    # Check for common over-promising phrases
    $overPromises = @(
        "AtomExecutionEngine",
        "MomentManagement", 
        "CampaignController",
        "UserRepository",
        "complete implementation",
        "fully functional"
    )
    
    foreach ($phrase in $overPromises) {
        if ($content -match $phrase) {
            $syncIssues += "❌ $doc mentions '$phrase' but may not exist"
        }
    }
}

if ($syncIssues.Count -gt 0) {
    Write-Host "`n🚨 SYNC ISSUES FOUND:" -ForegroundColor Red
    foreach ($issue in $syncIssues) {
        Write-Host $issue -ForegroundColor Yellow
    }
} else {
    Write-Host "`n✅ No obvious sync issues detected" -ForegroundColor Green
}

Write-Host "`n📊 SYNC HEALTH SCORE:" -ForegroundColor White
$healthScore = [math]::Max(0, 100 - ($syncIssues.Count * 10))
Write-Host "$healthScore/100" -ForegroundColor $(if($healthScore -gt 70) {"Green"} elseif($healthScore -gt 40) {"Yellow"} else {"Red"})

Write-Host "`n🎯 TO IMPROVE SYNC:" -ForegroundColor Cyan
Write-Host "1. Remove mentions of unbuilt features from docs" -ForegroundColor White
Write-Host "2. Update ACTUAL_STATUS.md weekly" -ForegroundColor White
Write-Host "3. Only document working features" -ForegroundColor White
Write-Host "4. Separate plans from current reality" -For