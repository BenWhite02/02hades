# =============================================================================
# IMMEDIATE SYNCHRONIZATION ACTIONS
# Author: Sankhadeep Banerjee
# Purpose: Scripts to align documentation with reality
# =============================================================================

# 1. CREATE ACTUAL STATUS AUDIT
# File: audit-actual-status.ps1

Write-Host "🔍 AUDITING ACTUAL PROJECT STATUS..." -ForegroundColor Cyan

# HADES Backend Audit
Write-Host "`n🔥 HADES BACKEND - ACTUAL FILES:" -ForegroundColor Red
$hadesPath = "src/main/kotlin/com/kairos/hades"

if (Test-Path $hadesPath) {
    Write-Host "✅ Main package exists" -ForegroundColor Green
    
    # Check main application
    if (Test-Path "$hadesPath/HadesApplication.kt") {
        Write-Host "✅ HadesApplication.kt - EXISTS AND WORKING" -ForegroundColor Green
    }
    
    # Check config files
    $configPath = "$hadesPath/config"
    if (Test-Path $configPath) {
        $configFiles = Get-ChildItem $configPath -Name "*.kt"
        Write-Host "✅ Config package: $($configFiles.Count) files" -ForegroundColor Green
        foreach ($file in $configFiles) {
            Write-Host "   - $file" -ForegroundColor White
        }
    }
    
    # Check empty packages
    $emptyPackages = @("entity", "enums", "multitenancy")
    foreach ($package in $emptyPackages) {
        $packagePath = "$hadesPath/$package"
        if (Test-Path $packagePath) {
            $files = Get-ChildItem $packagePath -Name "*.kt"
            if ($files.Count -eq 0) {
                Write-Host "❌ $package/ - EMPTY FOLDER (no .kt files)" -ForegroundColor Yellow
            } else {
                Write-Host "✅ $package/ - $($files.Count) files" -ForegroundColor Green
            }
        } else {
            Write-Host "❌ $package/ - FOLDER MISSING" -ForegroundColor Red
        }
    }
} else {
    Write-Host "❌ Hades source path not found!" -ForegroundColor Red
}

# KAIROS Frontend Audit  
Write-Host "`n⏰ KAIROS FRONTEND - ACTUAL FILES:" -ForegroundColor Blue
$kairosPath = "src"

if (Test-Path $kairosPath) {
    Write-Host "✅ Source directory exists" -ForegroundColor Green
    
    # Check main files
    $mainFiles = @("App.tsx", "main.tsx", "index.css")
    foreach ($file in $mainFiles) {
        if (Test-Path "$kairosPath/$file") {
            Write-Host "✅ $file - EXISTS" -ForegroundColor Green
        } else {
            Write-Host "❌ $file - MISSING" -ForegroundColor Red
        }
    }
    
    # Check key directories
    $directories = @("components", "pages", "hooks", "contexts")
    foreach ($dir in $directories) {
        $dirPath = "$kairosPath/$dir"
        if (Test-Path $dirPath) {
            $files = Get-ChildItem $dirPath -Recurse -Name "*.tsx", "*.ts"
            Write-Host "✅ $dir/ - $($files.Count) files" -ForegroundColor Green
        } else {
            Write-Host "❌ $dir/ - MISSING" -ForegroundColor Red
        }
    }
} else {
    Write-Host "❌ Kairos source path not found!" -ForegroundColor Red
}

# Build Status Check
Write-Host "`n🏗️ BUILD STATUS CHECK:" -ForegroundColor Magenta

# Check if Hades builds
Set-Location "." # Assuming in Hades directory
if (Test-Path "gradlew.bat") {
    Write-Host "🔧 Testing Hades build..." -ForegroundColor Yellow
    try {
        $buildResult = & ./gradlew.bat build -q 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ HADES - BUILDS SUCCESSFULLY" -ForegroundColor Green
        } else {
            Write-Host "❌ HADES - BUILD FAILS" -ForegroundColor Red
            Write-Host "Build errors: $buildResult" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ HADES - BUILD ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "❌ No gradlew.bat found - cannot test Hades build" -ForegroundColor Red
}

# Summary
Write-Host "`n📊 REALITY CHECK SUMMARY:" -ForegroundColor White
Write-Host "=" * 50 -ForegroundColor Gray
Write-Host "🔥 HADES: Basic Spring Boot app with config files only" -ForegroundColor Red
Write-Host "⏰ KAIROS: React app with basic structure" -ForegroundColor Blue
Write-Host "🔗 INTEGRATION: None (no working connection)" -ForegroundColor Yellow
Write-Host "📈 ESTIMATED COMPLETION: ~8% of planned roadmap" -ForegroundColor White
Write-Host "=" * 50 -ForegroundColor Gray

Write-Host "`n🎯 NEXT ACTIONS NEEDED:" -ForegroundColor Cyan
Write-Host "1. Create one working entity in Hades" -ForegroundColor White
Write-Host "2. Create one working REST endpoint" -ForegroundColor White  
Write-Host "3. Connect Kairos frontend to real API" -ForegroundColor White
Write-Host "4. Update documentation to match reality" -ForegroundColor White
