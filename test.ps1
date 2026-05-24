# demo20 API Test Script
# Usage: .\test.ps1

$ErrorActionPreference = "Stop"

function Write-Section($title) {
    Write-Host "`n===== $title =====" -ForegroundColor Cyan
}

function Write-Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }
function Write-Info($msg) { Write-Host "       $msg" -ForegroundColor Gray }

# ── Config ────────────────────────────────────────────────────────────────────
$keycloakBase   = "http://localhost:8080"
$realm          = "demo-realm"
$clientId       = "demo-app"
$clientSecret   = "demo-secret"
$username       = "user1"
$password       = "password1"

$gatewayBase    = "http://localhost:8072"
$mgmtBase       = "http://localhost:8079"
$orgBase        = "http://localhost:8073"
$frontendBase   = "http://localhost:8074"
$basicUser      = "admin"
$basicPass      = "admin-secret"

$basicToken = [Convert]::ToBase64String(
    [Text.Encoding]::ASCII.GetBytes("${basicUser}:${basicPass}"))

# ── 1. Actuator (Basic Auth) ──────────────────────────────────────────────────
Write-Section "1. Actuator endpoints (Basic Auth on :8079)"

try {
    $r = Invoke-RestMethod "$mgmtBase/actuator/health" `
         -Headers @{ Authorization = "Basic $basicToken" }
    Write-Pass "GET /actuator/health  status=$($r.status)"
} catch { Write-Fail "GET /actuator/health  $_" }

try {
    $routes = Invoke-RestMethod "$mgmtBase/actuator/gateway/routes" `
              -Headers @{ Authorization = "Basic $basicToken" }
    Write-Pass "GET /actuator/gateway/routes  count=$($routes.Count)"
    foreach ($route in $routes) {
        Write-Info "  id=$($route.route_id)  uri=$($route.uri)"
    }
} catch { Write-Fail "GET /actuator/gateway/routes  $_" }

# ── 2. Get JWT token (Keycloak ROPC) ─────────────────────────────────────────
Write-Section "2. Get JWT token from Keycloak"

try {
    $tokenResponse = Invoke-RestMethod `
        "$keycloakBase/realms/$realm/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            grant_type    = "password"
            client_id     = $clientId
            client_secret = $clientSecret
            username      = $username
            password      = $password
            scope         = "openid"
        }
    $token = $tokenResponse.access_token
    Write-Pass "Token acquired  expires_in=$($tokenResponse.expires_in)s"
    Write-Info "Token (first 60 chars): $($token.Substring(0,60))..."
} catch {
    Write-Fail "Cannot get token: $_"
    Write-Host "Remaining tests skipped." -ForegroundColor Yellow
    exit 1
}

$authHeader = @{ Authorization = "Bearer $token" }

# ── 3. Organization Service (direct :8073) ────────────────────────────────────
Write-Section "3. Organization Service (direct :8073)"

try {
    $orgs = Invoke-RestMethod "$orgBase/api/organizations" -Headers $authHeader
    Write-Pass "GET /api/organizations  count=$($orgs.Count)"
    foreach ($o in $orgs) {
        Write-Info "  id=$($o.id)  name=$($o.name)"
    }
} catch { Write-Fail "GET /api/organizations  $_" }

try {
    $firstId = "e6a625cc-718b-48c2-ac76-1dfdff9a531e"
    $org = Invoke-RestMethod "$orgBase/api/organizations/$firstId" -Headers $authHeader
    Write-Pass "GET /api/organizations/$firstId  name=$($org.name)"
} catch { Write-Fail "GET /api/organizations/{id}  $_" }

try {
    Invoke-WebRequest "$orgBase/api/organizations/does-not-exist" `
        -Headers $authHeader -UseBasicParsing | Out-Null
    Write-Fail "Expected 404, but got 2xx"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 404) {
        Write-Pass "GET /api/organizations/does-not-exist → 404 (expected)"
    } else {
        Write-Fail "Expected 404, got $code"
    }
}

# ── 4. Web Frontend (direct :8074) ───────────────────────────────────────────
Write-Section "4. Web Frontend (direct :8074)"

try {
    $r = Invoke-WebRequest "$frontendBase/" -Headers $authHeader -UseBasicParsing
    if ($r.Content -match "Organizations") {
        Write-Pass "GET /  HTML contains 'Organizations'"
    } else {
        Write-Fail "GET /  unexpected content"
        Write-Info $r.Content.Substring(0, [Math]::Min(200, $r.Content.Length))
    }
} catch { Write-Fail "GET /  $_" }

# ── 5. Actuator without credentials (should be 401) ──────────────────────────
Write-Section "5. Actuator without credentials → expect 401"

try {
    Invoke-WebRequest "$mgmtBase/actuator/health" -UseBasicParsing | Out-Null
    Write-Fail "Expected 401, but request succeeded without credentials"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 401) {
        Write-Pass "No credentials → 401 Unauthorized (correct)"
    } else {
        Write-Fail "Expected 401, got $code"
    }
}

Write-Host "`nDone." -ForegroundColor Cyan
