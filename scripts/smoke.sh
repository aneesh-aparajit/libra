#!/usr/bin/env bash
# End-to-end smoke test for a running libra API.
#
#   scripts/smoke.sh [base_url]     # default http://localhost:8080
#
# Every run creates its own tenant, so it is safe to repeat against the same database.
# Checks status codes and, for /v1/evaluate, the decision itself. Exits non-zero on any failure.
# Needs curl and jq.
set -u

BASE=${1:-http://localhost:8080}
for bin in curl jq; do
  command -v "$bin" >/dev/null || { echo "smoke: $bin is required" >&2; exit 2; }
done

RUN=$(date +%s)
NOW=$((RUN * 1000))
PAST=$((NOW - 3600000))
FUTURE=$((NOW + 86400000))
NONE=999999999
PASS=0
FAIL=0
OUT=$(mktemp)
trap 'rm -f "$OUT"' EXIT

pass() { PASS=$((PASS + 1)); printf '  ok    %s\n' "$1"; }
fail() { FAIL=$((FAIL + 1)); printf '  FAIL  %s\n        %s\n' "$1" "$2"; }

# call <expected_status> <label> <method> <path> [json_body]
call() {
  local expect=$1 label=$2 method=$3 path=$4 body=${5:-}
  local args=(-s -o "$OUT" -w '%{http_code}' -X "$method" "$BASE$path")
  [ -n "$body" ] && args+=(-H 'content-type: application/json' -d "$body")
  local code
  code=$(curl "${args[@]}") || code="curl-error"
  if [ "$code" = "$expect" ]; then
    pass "$label ($method $path -> $code)"
  else
    fail "$label ($method $path -> $code, expected $expect)" "$(head -c 400 "$OUT")"
  fi
}

# check <label> <got> <want>
check() {
  if [ "$2" = "$3" ]; then pass "$1 = $3"; else fail "$1: got '$2', expected '$3'" "$(head -c 400 "$OUT")"; fi
}

field() { jq -r "$@" "$OUT" 2>/dev/null; }
policy_result() { field --arg c "$1" '.evaluations[] | select(.policyCode == $c) | .result'; }

# evaluate <label> <entityID> <inputAttrs json> <expected overall result>
evaluate() {
  call 200 "$1" POST /v1/evaluate \
    "{\"tenantID\":$TID,\"scopeID\":$SID,\"entityID\":\"$2\",\"inputAttrs\":$3}"
  check "  result" "$(field .result)" "$4"
}

# policy <label> <entityID> <code> <evaluatedJs> [extra json fields]
policy() {
  call 201 "$1" POST /v1/policies \
    "{\"tenantID\":$TID,\"scopeID\":$SID,\"entityID\":\"$2\",\"code\":\"$3\",\"evaluatedJs\":\"$4\",\"validFrom\":$PAST${5:+,$5}}"
}

echo "libra smoke test against $BASE"

echo "health"
call 200 "healthz" GET /healthz
check "  status" "$(field .status)" "UP"

echo "tenants"
call 201 "create tenant" POST /v1/tenants "{\"tenantCode\":\"smoke-$RUN\",\"tenantName\":\"Smoke $RUN\"}"
TID=$(field .id)
call 200 "get tenant" GET "/v1/tenants/$TID"
call 200 "lookup tenant by code" GET "/v1/tenants?code=smoke-$RUN"
check "  id" "$(field .id)" "$TID"
call 404 "missing tenant" GET "/v1/tenants/$NONE"
call 400 "blank tenantCode" POST /v1/tenants '{"tenantCode":"","tenantName":"x"}'
call 400 "malformed json" POST /v1/tenants '{"tenantCode":'

echo "scopes"
call 201 "create scope" POST /v1/scopes "{\"tenantID\":$TID,\"scopeName\":\"notification\"}"
SID=$(field .id)
call 200 "get scope" GET "/v1/scopes/$SID"
call 200 "list scopes of tenant" GET "/v1/scopes?tenantID=$TID"
check "  count" "$(field length)" "1"
call 409 "duplicate scope name" POST /v1/scopes "{\"tenantID\":$TID,\"scopeName\":\"notification\"}"
call 400 "scope with unknown tenant" POST /v1/scopes "{\"tenantID\":$NONE,\"scopeName\":\"x\"}"
call 404 "missing scope" GET "/v1/scopes/$NONE"

echo "policies"
# scope-wide, no appliedJs (defaults to true), returns a boolean
policy "create scope-wide policy" '*' amount-limit \
  'function evaluate(input) { return input.amount <= 100; }'
PID_WIDE=$(field .id)
V_WIDE=$(field .version)
# entity-specific, explicit appliedJs, returns a boolean
policy "create entity policy" user-42 no-sms \
  'function evaluate(input) { return input.channel !== \"sms\"; }' \
  "\"appliedJs\":\"function evaluate(input) { return true; }\",\"validTo\":$FUTURE"
PID_ENT=$(field .id)
call 409 "duplicate tenant+scope+entity" POST /v1/policies \
  "{\"tenantID\":$TID,\"scopeID\":$SID,\"entityID\":\"user-42\",\"code\":\"dup\",\"evaluatedJs\":\"function evaluate() { return true; }\",\"validFrom\":$PAST}"
call 400 "validTo before validFrom" POST /v1/policies \
  "{\"tenantID\":$TID,\"scopeID\":$SID,\"entityID\":\"u\",\"code\":\"bad\",\"evaluatedJs\":\"function evaluate() { return true; }\",\"validFrom\":$NOW,\"validTo\":$PAST}"
call 400 "EXPIRED status on create" POST /v1/policies \
  "{\"tenantID\":$TID,\"scopeID\":$SID,\"entityID\":\"u\",\"code\":\"bad\",\"evaluatedJs\":\"function evaluate() { return true; }\",\"validFrom\":$PAST,\"status\":\"EXPIRED\"}"
call 400 "unknown scope" POST /v1/policies \
  "{\"tenantID\":$TID,\"scopeID\":$NONE,\"entityID\":\"u\",\"code\":\"bad\",\"evaluatedJs\":\"function evaluate() { return true; }\",\"validFrom\":$PAST}"
call 200 "get policy" GET "/v1/policies/$PID_WIDE"
check "  status defaults" "$(field .status)" "ACTIVE"
call 404 "missing policy" GET "/v1/policies/$NONE"
call 200 "list by tenantID+scopeID" GET "/v1/policies?tenantID=$TID&scopeID=$SID"
check "  count" "$(field length)" "2"
call 200 "list by entityID+scopeID" GET "/v1/policies?entityID=user-42&scopeID=$SID"
check "  count" "$(field length)" "1"
call 400 "list without filter" GET /v1/policies
call 200 "applicable to user-42" GET "/v1/policies/applicable?tenantID=$TID&scopeID=$SID&entityID=user-42"
check "  count" "$(field length)" "2"
call 200 "applicable to user-7" GET "/v1/policies/applicable?tenantID=$TID&scopeID=$SID&entityID=user-7"
check "  count" "$(field length)" "1"

echo "evaluate"
evaluate "user-7 under the limit" user-7 '{"amount":50}' ALLOW
evaluate "user-7 over the limit" user-7 '{"amount":500}' DENY
check "  deniedPolicies" "$(field -c .deniedPolicies)" '["amount-limit"]'
evaluate "user-42 via sms" user-42 '{"amount":10,"channel":"sms"}' DENY
check "  no-sms" "$(policy_result no-sms)" "DENY"
evaluate "user-42 via email" user-42 '{"amount":10,"channel":"email"}' ALLOW
check "  no-sms" "$(policy_result no-sms)" "ALLOW"
call 400 "evaluate without entityID" POST /v1/evaluate "{\"tenantID\":$TID,\"scopeID\":$SID}"

echo "updates"
call 200 "PUT with current version" PUT "/v1/policies/$PID_WIDE" \
  "{\"evaluatedJs\":\"function evaluate(input) { return input.amount <= 1000; }\",\"status\":\"ACTIVE\",\"validFrom\":$PAST,\"version\":$V_WIDE}"
check "  version" "$(field .version)" "$((V_WIDE + 1))"
call 409 "PUT with stale version" PUT "/v1/policies/$PID_WIDE" \
  "{\"evaluatedJs\":\"function evaluate() { return true; }\",\"status\":\"ACTIVE\",\"validFrom\":$PAST,\"version\":$V_WIDE}"
call 404 "PUT missing policy" PUT "/v1/policies/$NONE" \
  "{\"evaluatedJs\":\"function evaluate() { return true; }\",\"status\":\"ACTIVE\",\"validFrom\":$PAST,\"version\":1}"
evaluate "user-7 after raising the limit" user-7 '{"amount":500}' ALLOW
call 200 "deactivate entity policy" PATCH "/v1/policies/$PID_ENT/status" '{"status":"INACTIVE"}'
check "  status" "$(field .status)" "INACTIVE"
evaluate "user-42 via sms with policy inactive" user-42 '{"amount":10,"channel":"sms"}' ALLOW
check "  evaluations" "$(field '.evaluations | length')" "1"
call 400 "unknown status value" PATCH "/v1/policies/$PID_ENT/status" '{"status":"BOGUS"}'
call 404 "PATCH missing policy" PATCH "/v1/policies/$NONE/status" '{"status":"ACTIVE"}'

echo "failing js"
policy "create policy whose js throws" user-err throws \
  'function evaluate(input) { throw new Error(\"boom\"); }' \
  '"appliedJs":"function evaluate() { return true; }"'
evaluate "throwing policy fails closed" user-err '{"amount":1}' DENY
check "  throws" "$(policy_result throws)" "DENY"
check "  error recorded" "$(field --arg c throws '.evaluations[] | select(.policyCode == $c) | .errors != null')" "true"

echo
echo "$PASS passed, $FAIL failed (tenant $TID, scope $SID)"
[ "$FAIL" -eq 0 ]
