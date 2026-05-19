param(
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
$MODULE = Split-Path -Parent $PSScriptRoot
$AGENT_PROJECT = Split-Path -Parent $MODULE
$AGENT_JAR = Join-Path $AGENT_PROJECT 'agent-bootstrap\build\libs\agent-bootstrap-1.0-SNAPSHOT.jar'
$LIB_DIR = Join-Path $MODULE 'build\install\seeker-test-scenario\lib'
$CONFIG_DIR = Join-Path $MODULE 'src\main\resources\config'
$MAIN_CLASS = 'com.seeker.scenario.ScenarioApplication'

if (-not $NoBuild) {
    Write-Host '== gradlew :agent-bootstrap:shadowJar :seeker-test-scenario:installDist ==' -ForegroundColor Cyan
    Push-Location $AGENT_PROJECT
    try {
        .\gradlew.bat ':agent-bootstrap:shadowJar' ':seeker-test-scenario:installDist'
        if ($LASTEXITCODE -ne 0) { throw 'gradle build failed' }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $AGENT_JAR)) { throw "agent jar not found: $AGENT_JAR" }
if (-not (Test-Path $LIB_DIR)) { throw "lib dir not found: $LIB_DIR (gradle installDist 실패?)" }

# Spring Boot fat jar 대신 일반 classpath 실행.
# tomcat-embed-core 가 system classloader 에 올라와야 seeker-agent 의
# tomcat plugin 이 org.apache.catalina.connector.Request 를 인식한다.
$CLASSPATH = Join-Path $LIB_DIR '*'

function Start-Svc([string]$Name, [int]$Port, [string]$Config) {
    $cfg = Join-Path $CONFIG_DIR $Config
    if (-not (Test-Path $cfg)) { throw "config not found: $cfg" }
    $jvmArgs = @(
        "-javaagent:$AGENT_JAR",
        "-Dseeker.config=$cfg",
        "-Dserver.port=$Port",
        "-Dspring.application.name=$Name",
        '-cp', $CLASSPATH,
        $MAIN_CLASS
    )
    Write-Host ("-> starting {0,-15} port {1}" -f $Name, $Port) -ForegroundColor Green
    Start-Process -FilePath 'java' -ArgumentList $jvmArgs -WindowStyle Minimized | Out-Null
}

Start-Svc 'API-GATEWAY'     8090 'gateway.config'
Start-Sleep -Seconds 3
Start-Svc 'AUTH-SERVICE'    8091 'auth.config'
Start-Svc 'ORDER-SERVICE'   8092 'order.config'
Start-Svc 'PRODUCT-SERVICE' 8093 'product.config'
Start-Svc 'PAYMENT-SERVICE' 8094 'payment.config'
Start-Svc 'PG-MOCK'         8095 'pgmock.config'

Write-Host ''
Write-Host '6개 인스턴스 시작 요청 완료. 부팅에 약 10~20초.' -ForegroundColor Cyan
Write-Host '검증: curl http://localhost:8090/api/products'
Write-Host '부하: .\scripts\load-runner.ps1'
Write-Host '종료: .\scripts\stop-all.ps1'
