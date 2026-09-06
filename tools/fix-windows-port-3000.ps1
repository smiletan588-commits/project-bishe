param(
    [Parameter(Mandatory = $true)]
    [string]$ResultFile
)

$ErrorActionPreference = 'Stop'

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = [Security.Principal.WindowsPrincipal]::new($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    try {
        $arguments = @(
            '-NoProfile',
            '-ExecutionPolicy', 'Bypass',
            '-File', "`"$PSCommandPath`"",
            '-ResultFile', "`"$ResultFile`""
        )
        $process = Start-Process -FilePath 'powershell.exe' -Verb RunAs -WindowStyle Hidden -ArgumentList $arguments -Wait -PassThru
        exit $process.ExitCode
    }
    catch {
        exit 1
    }
}

$lines = [System.Collections.Generic.List[string]]::new()

function Add-Result([string]$Message) {
    $lines.Add("$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Message")
}

try {
    Add-Result 'Restoring the standard IPv4 TCP dynamic port range.'
    & netsh interface ipv4 set dynamicport tcp start=49152 num=16384 | ForEach-Object { Add-Result $_ }
    if ($LASTEXITCODE -ne 0) {
        throw "netsh set dynamicport failed with exit code $LASTEXITCODE."
    }

    foreach ($serviceName in @('winnat', 'hns')) {
        $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
        if ($service -and $service.Status -ne 'Stopped') {
            Add-Result "Stopping service $serviceName."
            Stop-Service -Name $serviceName -Force
            $service.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
        }
    }

    Add-Result 'Deleting the active TCP exclusion range 2935-3034.'
    & netsh interface ipv4 delete excludedportrange protocol=tcp startport=2935 numberofports=100 store=active | ForEach-Object { Add-Result $_ }
    $deleteExitCode = $LASTEXITCODE
    if ($deleteExitCode -ne 0) {
        Add-Result "The exclusion was already released or could not be deleted (exit $deleteExitCode); verifying current state."
    }

    Add-Result 'Port configuration repair completed. Docker Desktop can now be started.'
    $lines | Set-Content -LiteralPath $ResultFile -Encoding UTF8
    exit 0
}
catch {
    Add-Result "ERROR: $($_.Exception.Message)"
    $lines | Set-Content -LiteralPath $ResultFile -Encoding UTF8
    exit 1
}
