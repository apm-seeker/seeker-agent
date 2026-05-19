$ErrorActionPreference = 'SilentlyContinue'

$ports = 8090, 8091, 8092, 8093, 8094, 8095
foreach ($p in $ports) {
    $owner = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if (-not $owner) { continue }
    $targetPid = $owner.OwningProcess
    $proc = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
    if ($proc -and $proc.ProcessName -match 'java') {
        Write-Host ("kill {0,-6} pid={1}" -f $p, $targetPid) -ForegroundColor Yellow
        Stop-Process -Id $targetPid -Force
    }
}
Write-Host 'done.'
