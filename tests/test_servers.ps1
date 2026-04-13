$ProjectRoot = Get-Item .
$TestsRoot = New-Item -ItemType Directory -Force -Path "tests"
$BukkitJar = Join-Path $ProjectRoot "bukkit\build\libs\RedstoneReboot-Bukkit-1.3.1.jar"
$FabricJar = Join-Path $ProjectRoot "fabric\build\libs\RedstoneReboot-Fabric-1.3.1.jar"
$ForgeJar = Join-Path $ProjectRoot "forge\build\libs\RedstoneReboot-Forge-1.3.1.jar"
$NeoForgeJar = Join-Path $ProjectRoot "neoforge\build\libs\RedstoneReboot-NeoForge-1.3.1.jar"

function Start-TestServer {
    param(
        [string]$Name,
        [string]$Version,
        [string]$Artifact,
        [string]$DownloadUrl,
        [string]$JarName,
        [int]$Port,
        [string]$Type = "bukkit",
        [bool]$Cleanup = $true,
        [string]$FabricApiUrl = ""
    )

    Write-Host "`n--- Testing $Name ($Version) [$Type] on Port $Port ---" -ForegroundColor Cyan
    $ServerDir = Join-Path $TestsRoot $Name
    if (!(Test-Path $ServerDir)) { New-Item -ItemType Directory -Path $ServerDir }
    
    $JarPath = Join-Path $ServerDir $JarName
    if (!(Test-Path $JarPath)) {
        Write-Host "Downloading $Name ($Version)..."
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $JarPath
    }

    $ModsDir = if ($Type -eq "bukkit") { Join-Path $ServerDir "plugins" } else { Join-Path $ServerDir "mods" }
    if (!(Test-Path $ModsDir)) { New-Item -ItemType Directory -Path $ModsDir }
    
    # Debug artifact existence
    if (Test-Path $Artifact) {
        Write-Host "Found artifact at $Artifact" -ForegroundColor Gray
    } else {
        Write-Host "ERROR: Artifact NOT found at $Artifact" -ForegroundColor Red
        return $false
    }

    Copy-Item $Artifact (Join-Path $ModsDir "RedstoneReboot.jar") -Force
    
    if ($FabricApiUrl -and $Type -eq "fabric") {
        $ApiJar = Join-Path $ModsDir "fabric-api.jar"
        if (!(Test-Path $ApiJar)) {
            Write-Host "Downloading Fabric API..."
            Invoke-WebRequest -Uri $FabricApiUrl -OutFile $ApiJar
        }
    }

    Set-Content -Path (Join-Path $ServerDir "eula.txt") -Value "eula=true"
    Set-Content -Path (Join-Path $ServerDir "server.properties") -Value "server-port=$Port`nquery.port=$Port`nonline-mode=false"

    $LogDir = Join-Path $ServerDir "logs"
    if (!(Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir }
    $LogFile = Join-Path $LogDir "latest.log"
    Set-Content -Path $LogFile -Value ""

    Write-Host "Starting $Name..."
    $Process = Start-Process "java" -ArgumentList "-Xmx1G -jar $JarName nogui" -WorkingDirectory $ServerDir -PassThru -NoNewWindow
    
    $StartTime = Get-Date
    $TimeoutSeconds = 120
    $Success = $false
    $CommandVerified = $false

    while (((Get-Date) - $StartTime).TotalSeconds -lt $TimeoutSeconds) {
        if ($Process.HasExited) {
            Write-Host "$Name process exited unexpectedly." -ForegroundColor Red
            break
        }

        if (Test-Path $LogFile) {
            $Content = Get-Content $LogFile -Raw
            if ($Content -match "Engine initialized successfully") {
                $Success = $true
                Write-Host "  [OK] Plugin initialized." -ForegroundColor Green
            }
            if ($Content -match "registered command: restart" -or $Content -match "RedstoneReboot command registered") {
                $CommandVerified = $true
                Write-Host "  [OK] Command verified." -ForegroundColor Green
            }
        }
        
        if ($Success -and ($Type -eq "bukkit" -or $CommandVerified)) { break }
        Start-Sleep -Seconds 5
    }

    Write-Host "Stopping $Name..."
    try { $Process.Kill() } catch {}
    Start-Sleep -Seconds 2

    if ($Cleanup) {
        Write-Host "Cleaning up $Name..."
        Remove-Item -Path $ServerDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    return $Success
}

$Results = @{}

# BUKKIT
$Results["Paper-1.21.1"] = Start-TestServer -Name "Paper-1.21.1" -Version "1.21.1" -Artifact $BukkitJar -DownloadUrl "https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/133/downloads/paper-1.21.1-133.jar" -JarName "paper.jar" -Port 25565
$Results["Paper-1.12.2"] = Start-TestServer -Name "Paper-1.12.2" -Version "1.12.2" -Artifact $BukkitJar -DownloadUrl "https://api.papermc.io/v2/projects/paper/versions/1.12.2/builds/1620/downloads/paper-1.12.2-1620.jar" -JarName "paper.jar" -Port 25566

# FABRIC (With Fabric API)
$Results["Fabric-1.20.1"] = Start-TestServer -Name "Fabric-1.20.1" -Version "1.20.1" -Artifact $FabricJar -DownloadUrl "https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.15.11/1.0.1/server/jar" -JarName "fabric-server.jar" -Port 25567 -Type "fabric" -FabricApiUrl "https://github.com/FabricMC/fabric-api/releases/download/0.92.0+1.20.1/fabric-api-0.92.0+1.20.1.jar"
$Results["Fabric-1.21.1"] = Start-TestServer -Name "Fabric-1.21.1" -Version "1.21.1" -Artifact $FabricJar -DownloadUrl "https://meta.fabricmc.net/v2/versions/loader/1.21.1/0.16.5/1.0.1/server/jar" -JarName "fabric-server.jar" -Port 25568 -Type "fabric" -FabricApiUrl "https://github.com/FabricMC/fabric-api/releases/download/0.113.1+1.21.1/fabric-api-0.113.1+1.21.1.jar"

Write-Host "`n==========================================" -ForegroundColor Yellow
Write-Host "   Multi-Platform Verification Summary" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
$Results.GetEnumerator() | ForEach-Object {
    $Status = if ($_.Value) { "PASS" } else { "FAIL" }
    $Color = if ($_.Value) { "Green" } else { "Red" }
    Write-Host "$($_.Key.PadRight(15)): $Status" -ForegroundColor $Color
}
Write-Host "==========================================" -ForegroundColor Yellow
