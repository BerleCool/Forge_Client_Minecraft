# Windows equivalent of the checksum-verified bootstrap in ../gradlew.
$ErrorActionPreference = "Stop"
$Version = "8.8"
$Expected = "a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612"
$HomeDir = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE ".gradle" }
$Cache = Join-Path $HomeDir "forge-bootstrap"
$Gradle = Join-Path $Cache "gradle-$Version\bin\gradle.bat"
if (-not (Test-Path $Gradle)) {
    New-Item -ItemType Directory -Force -Path $Cache | Out-Null
    $Temp = Join-Path $Cache ([System.Guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $Temp | Out-Null
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $Zip = Join-Path $Temp "gradle.zip"
        Invoke-WebRequest -UseBasicParsing -Uri "https://services.gradle.org/distributions/gradle-$Version-bin.zip" -OutFile $Zip
        if ((Get-FileHash $Zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) { throw "Gradle checksum mismatch" }
        Expand-Archive -Path $Zip -DestinationPath $Temp
        $Destination = Join-Path $Cache "gradle-$Version"
        if (-not (Test-Path $Destination)) { Move-Item (Join-Path $Temp "gradle-$Version") $Destination }
    } finally { Remove-Item -Recurse -Force $Temp }
}
Push-Location (Join-Path $PSScriptRoot "..")
try { & $Gradle @args; $Code = $LASTEXITCODE } finally { Pop-Location }
exit $Code
