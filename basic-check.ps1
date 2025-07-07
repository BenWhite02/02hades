# =============================================================================
# BASIC HADES STRUCTURE CHECK (NO UNICODE)
# File: basic-check.ps1
# Author: Sankhadeep Banerjee
# =============================================================================

Write-Host "HADES PROJECT STRUCTURE CHECK" -ForegroundColor Red
Write-Host "==============================" -ForegroundColor Gray

# Check main source directory
$srcPath = "src/main/kotlin/com/kairos/hades"
Write-Host ""
Write-Host "Checking: $srcPath" -ForegroundColor Cyan

if (Test-Path $srcPath) {
    Write-Host "[OK] Main source directory EXISTS" -ForegroundColor Green
    
    # List all files recursively
    Write-Host ""
    Write-Host "ALL FILES IN HADES:" -ForegroundColor Yellow
    Get-ChildItem -Path $srcPath -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Replace("$PWD\", "")
        Write-Host "  FILE: $relativePath" -ForegroundColor White
    }
    
    # Count files by type
    $ktFiles = Get-ChildItem -Path $srcPath -Recurse -Filter "*.kt"
    Write-Host ""
    Write-Host "SUMMARY:" -ForegroundColor Cyan
    Write-Host "  Kotlin files: $($ktFiles.Count)" -ForegroundColor White
    
} else {
    Write-Host "[ERROR] Main source directory NOT FOUND" -ForegroundColor Red
}

# Check resources
$resourcesPath = "src/main/resources"
Write-Host ""
Write-Host "Checking: $resourcesPath" -ForegroundColor Cyan

if (Test-Path $resourcesPath) {
    Write-Host "[OK] Resources directory EXISTS" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "RESOURCES FILES:" -ForegroundColor Yellow
    Get-ChildItem -Path $resourcesPath -File | ForEach-Object {
        Write-Host "  FILE: $($_.Name)" -ForegroundColor White
    }
} else {
    Write-Host "[ERROR] Resources directory NOT FOUND" -ForegroundColor Red
}

# Check build files
Write-Host ""
Write-Host "BUILD FILES:" -ForegroundColor Cyan
$buildFiles = @("build.gradle.kts", "settings.gradle.kts", "gradlew.bat")
foreach ($file in $buildFiles) {
    if (Test-Path $file) {
        Write-Host "[OK] $file" -ForegroundColor Green
    } else {
        Write-Host "[MISSING] $file" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==============================" -ForegroundColor Gray
Write-Host "Structure check complete!" -ForegroundColor Green

# Show current directory for context
Write-Host ""
Write-Host "Current directory: $PWD" -ForegroundColor Cyan
Write-Host "Directory contents:" -ForegroundColor Cyan
Get-ChildItem -Name | ForEach-Object {
    Write-Host "  $_" -ForegroundColor Gray
}