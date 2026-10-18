[CmdletBinding()]
param()

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repositoryRoot = (Resolve-Path (Join-Path $projectRoot '..')).Path
$mappedDrive = $null
$originalLocation = Get-Location

try {
    $buildRoot = $projectRoot
    if ($projectRoot -match '[^\x00-\x7F]') {
        $mappedDrive = @('W', 'V', 'U', 'T') |
            Where-Object { -not (Test-Path "${_}:\") } |
            Select-Object -First 1
        if (-not $mappedDrive) {
            throw 'No free drive letter is available for the ASCII path workaround.'
        }
        & subst.exe "${mappedDrive}:" $repositoryRoot
        if ($LASTEXITCODE -ne 0) {
            throw 'Failed to create the temporary drive mapping.'
        }
        $buildRoot = "${mappedDrive}:\android-app"
    }

    Set-Location $buildRoot
    & .\gradlew.bat clean testDebugUnitTest assembleDebug lintDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Android verification failed with exit code $LASTEXITCODE."
    }
} finally {
    Set-Location $originalLocation
    if ($mappedDrive) {
        & subst.exe "${mappedDrive}:" /D
    }
}
