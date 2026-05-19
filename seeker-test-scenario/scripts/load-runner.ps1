<#
운영 e-commerce 트래픽 모사 부하 러너.

가중치 (총 100):
  GET  /api/products         30   (가벼움)
  GET  /api/products/{id}    20   (가벼움)
  GET  /api/users/me         15   (중간)
  GET  /api/orders           15   (중간)
  POST /api/auth/login       10   (가벼움)
  POST /api/orders           10   (무거움 — 전체 체크아웃 체인)

다양한 trace pattern + 가끔 chaos (downstream 자체) 발생.

사용 예:
  .\load-runner.ps1
  .\load-runner.ps1 -IntervalMs 300
  .\load-runner.ps1 -Count 500
#>
param(
    [int]$IntervalMs = 600,
    [int]$Count = 0
)

$gateway = 'http://localhost:8090/api'

# 사용자 → token (DataSeeder seed 와 일치)
$users = @(
    @{ name = 'alice'; token = 'TOKEN-ALICE' }
    @{ name = 'bob';   token = 'TOKEN-BOB' }
    @{ name = 'carol'; token = 'TOKEN-CAROL' }
)

# 가중치 풀
$pool = @()
$pool += (1..30 | ForEach-Object { 'products' })
$pool += (1..20 | ForEach-Object { 'product-one' })
$pool += (1..15 | ForEach-Object { 'me' })
$pool += (1..15 | ForEach-Object { 'orders-list' })
$pool += (1..10 | ForEach-Object { 'login' })
$pool += (1..10 | ForEach-Object { 'orders-create' })

function Invoke-Call([string]$Op, [hashtable]$User) {
    $hdrs = @{ 'Authorization' = "Bearer $($User.token)" }
    switch ($Op) {
        'products' {
            return @{ Method = 'GET'; Uri = "$gateway/products" }
        }
        'product-one' {
            $productId = Get-Random -Min 1 -Max 6   # 1..5
            return @{ Method = 'GET'; Uri = "$gateway/products/$productId" }
        }
        'me' {
            return @{ Method = 'GET'; Uri = "$gateway/users/me"; Headers = $hdrs }
        }
        'orders-list' {
            return @{ Method = 'GET'; Uri = "$gateway/orders"; Headers = $hdrs }
        }
        'login' {
            $body = @{ username = $User.name; password = 'x' } | ConvertTo-Json -Compress
            return @{ Method = 'POST'; Uri = "$gateway/auth/login"; Body = $body; ContentType = 'application/json' }
        }
        'orders-create' {
            $productId = Get-Random -Min 1 -Max 6
            $qty = Get-Random -Min 1 -Max 3
            $body = @{ productId = $productId; quantity = $qty } | ConvertTo-Json -Compress
            return @{ Method = 'POST'; Uri = "$gateway/orders"; Body = $body; ContentType = 'application/json'; Headers = $hdrs }
        }
    }
}

$i = 0
while ($Count -le 0 -or $i -lt $Count) {
    $op = $pool | Get-Random
    $user = $users | Get-Random
    $call = Invoke-Call -Op $op -User $user

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $r = Invoke-WebRequest @call -TimeoutSec 8 -ErrorAction Stop -UseBasicParsing
        $sw.Stop()
        Write-Host ("OK   {0,4}ms  {1,-14}  user={2,-5}  {3}" -f $sw.ElapsedMilliseconds, $op, $user.name, $r.StatusCode) -ForegroundColor Green
    } catch {
        $sw.Stop()
        $code = '???'
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        Write-Host ("FAIL {0,4}ms  {1,-14}  user={2,-5}  {3}" -f $sw.ElapsedMilliseconds, $op, $user.name, $code) -ForegroundColor Yellow
    }
    Start-Sleep -Milliseconds $IntervalMs
    $i++
}
