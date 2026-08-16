param(
    [int]$StartupTimeoutSeconds = 120,
    [switch]$KeepContainer
)

$ErrorActionPreference = "Stop"

$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$containerName = "ums-flyway-validation-$PID"
$mysqlPassword = "ums-flyway-validation"
$logDirectory = Join-Path $workspace ".runlogs\migration-validation"
$keyDirectory = Join-Path $logDirectory "jwt-$PID"
$privateKey = Join-Path $keyDirectory "private_key.pem"
$publicKey = Join-Path $keyDirectory "public_key.pem"

New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $keyDirectory | Out-Null

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is required for clean database validation."
}

$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $mavenCommand) {
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
}

$backendMavenWrapper = Join-Path $workspace "backend\mvnw.cmd"
if (-not $mavenCommand -and -not (Test-Path $backendMavenWrapper)) {
    throw "Maven or the backend Maven wrapper is required for clean database validation."
}

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) {
    throw "Java is required to generate temporary JWT validation keys."
}

function Initialize-SharedArtifacts {
    $mavenExecutable = if ($mavenCommand) { $mavenCommand.Source } else { $backendMavenWrapper }
    $backendPom = Join-Path $workspace "backend\pom.xml"

    & $mavenExecutable -q -f $backendPom -pl "common/common-events" -DskipTests install
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to install common-events into the local Maven repository."
    }
}

function New-TemporaryJwtKeys {
    $generatorPath = Join-Path $keyDirectory "GenerateJwtKeys.java"
    @'
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeys {
    private static String pem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Files.writeString(Path.of(args[0]), pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        Files.writeString(Path.of(args[1]), pem("PUBLIC KEY", pair.getPublic().getEncoded()));
    }
}
'@ | Set-Content -LiteralPath $generatorPath -Encoding ASCII

    & $javaCommand.Source $generatorPath $privateKey $publicKey
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $privateKey) -or -not (Test-Path $publicKey)) {
        throw "Failed to generate temporary JWT validation keys."
    }
}

$services = @(
    @{ Name = "authentication-service"; Database = "auth_db" },
    @{ Name = "user-service"; Database = "user_db" },
    @{ Name = "authorization-service"; Database = "authorization_db" },
    @{ Name = "organization-service"; Database = "organization_db" },
    @{ Name = "notification-service"; Database = "notification_db" },
    @{ Name = "audit-service"; Database = "audit_db" }
)

function Wait-ForMySql {
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    do {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"
        docker exec -e "MYSQL_PWD=$mysqlPassword" $containerName mysql -uroot --silent --skip-column-names -e "SELECT 1" 2>$null | Out-Null
        $mysqlExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference
        if ($mysqlExitCode -eq 0) {
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "Temporary MySQL did not become ready within $StartupTimeoutSeconds seconds."
}

function Wait-ForServiceBoot {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$LogPath,
        [string]$ServiceName
    )

    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    do {
        if (Test-Path $LogPath) {
            $content = Get-Content -Raw $LogPath
            if ($content -match "Started .+ in [0-9.]+ seconds") {
                return
            }
            if ($content -match "APPLICATION FAILED TO START|BUILD FAILURE") {
                throw "$ServiceName failed to boot. See $LogPath"
            }
        }
        if ($Process.HasExited) {
            throw "$ServiceName exited before reporting a successful boot. See $LogPath"
        }
        Start-Sleep -Seconds 2
        $Process.Refresh()
    } while ((Get-Date) -lt $deadline)

    throw "$ServiceName did not boot within $StartupTimeoutSeconds seconds. See $LogPath"
}

function Stop-ProcessTree {
    param([int]$ProcessId)

    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-ProcessTree -ProcessId $child.ProcessId
    }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

try {
    Initialize-SharedArtifacts
    New-TemporaryJwtKeys

    docker run -d --rm --name $containerName `
        -e "MYSQL_ROOT_PASSWORD=$mysqlPassword" `
        -p "127.0.0.1::3306" `
        mysql:8.0 | Out-Null

    Wait-ForMySql

    $portLine = docker port $containerName 3306/tcp
    $mysqlPort = [int](($portLine -split ":")[-1])

    $databaseSql = ($services | ForEach-Object {
        "CREATE DATABASE ``$($_.Database)`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    }) -join " "
    docker exec -e "MYSQL_PWD=$mysqlPassword" $containerName mysql -uroot -e $databaseSql

    foreach ($service in $services) {
        $serviceName = $service.Name
        $serviceDirectory = Join-Path $workspace "backend\$serviceName"
        $logPath = Join-Path $logDirectory "$serviceName.log"
        $errorLogPath = Join-Path $logDirectory "$serviceName.error.log"
        Remove-Item -LiteralPath $logPath, $errorLogPath -Force -ErrorAction SilentlyContinue

        $mavenExecutable = if ($mavenCommand) { $mavenCommand.Source } else { Join-Path $serviceDirectory "mvnw.cmd" }
        if (-not (Test-Path $mavenExecutable)) {
            throw "Maven or the Maven wrapper is required for $serviceName boot validation."
        }

        $arguments = @(
            "-q",
            "-DskipTests",
            "spring-boot:run",
            "`"-Dspring-boot.run.arguments=--spring.cloud.config.enabled=false --spring.config.import= --eureka.client.enabled=false --server.port=0 --spring.datasource.url=jdbc:mysql://127.0.0.1:$mysqlPort/$($service.Database)?useSSL=false&allowPublicKeyRetrieval=true --spring.datasource.username=root --spring.datasource.password=$mysqlPassword --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=true --spring.flyway.baseline-on-migrate=false --spring.rabbitmq.listener.simple.auto-startup=false --spring.rabbitmq.listener.direct.auto-startup=false --spring.task.scheduling.enabled=false --spring.devtools.restart.enabled=false --spring.mail.host=127.0.0.1 --management.health.rabbit.enabled=false --management.health.redis.enabled=false --management.health.mail.enabled=false --internal.gateway.secret=migration-validation-gateway --internal.service.secret=migration-validation-service --jwt.private-key-path=$privateKey --jwt.public-key-path=$publicKey --jwt.key-id=migration-validation`""
        )

        $process = Start-Process -FilePath $mavenExecutable `
            -ArgumentList $arguments `
            -WorkingDirectory $serviceDirectory `
            -RedirectStandardOutput $logPath `
            -RedirectStandardError $errorLogPath `
            -WindowStyle Hidden `
            -PassThru

        try {
            Wait-ForServiceBoot -Process $process -LogPath $logPath -ServiceName $serviceName
            Write-Host "[PASS] $serviceName booted against an empty $($service.Database) database."
        }
        finally {
            if (-not $process.HasExited) {
                Stop-ProcessTree -ProcessId $process.Id
            }
        }
    }

    Write-Host "All stateful services passed clean database migration validation."
}
finally {
    if (-not $KeepContainer) {
        docker rm -f $containerName 2>$null | Out-Null
    }
    Remove-Item -LiteralPath $keyDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
