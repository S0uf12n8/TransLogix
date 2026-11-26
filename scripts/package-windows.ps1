<#
.SYNOPSIS
    Builds TransLogix as a Windows application that runs without Java or Maven installed.

.DESCRIPTION
    1. mvnw package          -> target/translogix-<pom version>.jar + its dependencies in target/lib
    2. jdeps                 -> the Java modules the app really needs (keeps the bundled runtime small)
    3. jpackage app-image    -> target/dist/TransLogix/TransLogix.exe with its own Java runtime
    4. Compress-Archive      -> target/dist/TransLogix-<version>-windows.zip

    jpackage ships with JDK 21: nothing else to install (the "app-image" type needs no WiX).

.PARAMETER Version
    Version shown in the app's properties: 1 to 3 numbers, e.g. 0.3 or 0.3.1.

.PARAMETER SkipTests
    Skip the tests during the Maven build (CI already ran them).

.EXAMPLE
    .\scripts\package-windows.ps1 -Version 0.3
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+(\.\d+){0,2}$')]
    [string]$Version,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$racine = Split-Path -Parent $PSScriptRoot
Set-Location $racine

function Invoke-Etape([string]$nom, [scriptblock]$commande) {
    Write-Host "==> $nom"
    & $commande
    if ($LASTEXITCODE -ne 0) { throw "$nom a échoué (code $LASTEXITCODE)" }
}

# Tools from the JDK that runs this script (JAVA_HOME first, then the PATH).
$jdkBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { Split-Path -Parent (Get-Command java).Source }
$jdeps = Join-Path $jdkBin 'jdeps.exe'
$jpackage = Join-Path $jdkBin 'jpackage.exe'
if (-not (Test-Path $jpackage)) { throw "jpackage introuvable dans $jdkBin : il faut un JDK 21 complet (pas un JRE)." }

# A previous run leaves read-only files that "mvnw clean" cannot delete.
$dist = Join-Path target 'dist'
if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }

# 1. Build
$mvnArgs = @('-B', 'clean', 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
Invoke-Etape 'Build Maven' { & .\mvnw.cmd @mvnArgs }

$jar = Get-ChildItem target -Filter 'translogix-*.jar' | Select-Object -First 1
if (-not $jar) { throw 'Le jar de l''application est introuvable dans target/.' }

# jpackage takes a folder holding the app jar and everything it needs.
$entree = Join-Path target 'jpackage-input'
New-Item -ItemType Directory -Force (Join-Path $entree 'lib') | Out-Null
Copy-Item $jar.FullName $entree
Copy-Item (Join-Path target 'lib\*.jar') (Join-Path $entree 'lib')

# 2. Modules needed by the app and its libraries, plus the ones jdeps cannot see because they are only used
#    by the MySQL driver at connection time (java.naming, java.management: without them the app crashes as
#    soon as DB_URL is set) or loaded by name (French dates and numbers, charsets, TLS).
Write-Host '==> Modules Java (jdeps)'
$classpath = (Get-ChildItem (Join-Path $entree 'lib') -Filter *.jar | ForEach-Object FullName) -join ';'
$modules = & $jdeps --print-module-deps --ignore-missing-deps --multi-release 21 `
    --class-path $classpath (Join-Path $entree $jar.Name)
if ($LASTEXITCODE -ne 0 -or -not $modules) { throw 'jdeps n''a pas pu calculer les modules.' }
$modules = (($modules.Trim() -split ',') + @('java.naming', 'java.management', 'jdk.localedata', 'jdk.charsets', 'jdk.crypto.ec') |
    Where-Object { $_ -notlike 'javafx.*' } | Sort-Object -Unique) -join ','
Write-Host "    $modules"

# 3. Windows application with its own runtime
Invoke-Etape 'jpackage' {
    & $jpackage --type app-image `
        --name TransLogix `
        --app-version $Version `
        --vendor 'TransLogix' `
        --description 'Gestion des opérations de transport routier' `
        --input $entree `
        --main-jar $jar.Name `
        --main-class ma.translogix.Launcher `
        --add-modules $modules `
        --java-options '-Dfile.encoding=UTF-8' `
        --dest $dist
}
# jpackage marks the launcher read-only: "mvnw clean" would then fail for everyone on this machine.
Get-ChildItem $dist -Recurse -File | ForEach-Object { $_.IsReadOnly = $false }

# 4. Zip to attach to the GitHub Release
$zip = Join-Path $dist "TransLogix-$Version-windows.zip"
Write-Host "==> Archive $zip"
Compress-Archive -Path (Join-Path $dist 'TransLogix') -DestinationPath $zip -Force

Write-Host ''
Write-Host "OK : $zip"
Write-Host "Lancer sans dézipper : $(Join-Path $dist 'TransLogix\TransLogix.exe')"
