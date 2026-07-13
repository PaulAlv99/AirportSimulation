param(
    [switch] $Apply,
    [string] $TargetDirectory = "data/import/openflights"
)

$ErrorActionPreference = "Stop"

$files = @{
    "routes.dat" = "https://raw.githubusercontent.com/jpatokal/openflights/master/data/routes.dat"
    "airlines.dat" = "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat"
    "planes.dat" = "https://raw.githubusercontent.com/jpatokal/openflights/master/data/planes.dat"
}

if (-not $Apply) {
    Write-Host "Dry run. Re-run with -Apply to download OpenFlights files into $TargetDirectory."
    foreach ($entry in $files.GetEnumerator()) {
        Write-Host "$($entry.Name) <- $($entry.Value)"
    }
    exit 0
}

New-Item -ItemType Directory -Force -Path $TargetDirectory | Out-Null
foreach ($entry in $files.GetEnumerator()) {
    $target = Join-Path $TargetDirectory $entry.Name
    Invoke-WebRequest -Uri $entry.Value -OutFile $target
    Write-Host "Downloaded $target"
}
