libra is a small, multi-tenant policy evaluation engine. a client sends an entity and an input, and libra returns allow or
deny based on policies written as javascript functions.

# problem

authorization and business-rule checks end up hard-coded across services. changing one means redeploy, there's no audit
trail on why a request was denied, and temporary rules (a promo, a freeze window) get forgotten and are never removed.

## goals

- decouple decisions from enforcement: services call `/evaluate` , and libra decides.
- generic policies: two JS functions per policy, `applies()`  and `evaluate()` .
- evaluate all policies applicable to an entity together: every one must allow, and any deny or error means deny.
- time-bound policies with automatic expiry.
- explainable decisions: every response carries a trace.
- a k8s crd, so policies can be managed as a yaml.

## non-goals

- an rbac or authorization layer over libra itself. callers are trusted.
- rego or any other declarative language.
- storing entity data or scope membership. libra never interprets an identifier.

# concepts and data model

| notion | what it is                                                                                               |
|--------|----------------------------------------------------------------------------------------------------------|
| tenant | an organization, and the isolation boundary, every call is scoped within.                                |
| scope  | a group of users within a tenant, e.g. `notification`                                                    |
| entity | the identifier for the user, supplied by the client. libra doesn't care what it signifies for the client. |
| policy | a rule targeted at a whole scope or at one entity.                                                       |

## targeting

| tenant_id | scope        | identifier       | applies to                                                            |
|-----------|--------------|------------------|-----------------------------------------------------------------------|
| 1         | notification | *                | every user of the notification scope within tenant 1.                 |
| 1         | notification | account-holder-1 | only applicable to account-holder-1 of tenant 1's notification scope. |

## function contract

```
function applies(entity, ctx) {        // entity = { id, attributes }; ctx = { now, tenant, scope }
    return entity.attributes.region === "eu";
}

function evaluate(input, entity, ctx) { // input = request payload
    const hour = new Date(ctx.now).getUTCHours();
    return {allow: hour >= 8 && hour < 22, reason: "quiet hours"};
}
```

> both functions should be pure. no state kept between calls, no i/o, and the same result for the same arguement.
> storage is pgsql.

# db design

## tenants

| field         | type      | notes                |
|---------------|-----------|----------------------|
| `id`          | bigserial | pkey; autoincrement; |
| `tenant_code` | string    |                      |
| `tenant_name` | string    |                      |

- **constraints**
    - primary key (id)
    - index (tenant_code)

## policies

| field         | type   | notes                                                                                         |
|---------------|--------|-----------------------------------------------------------------------------------------------|
| `id`          | string | BIGSERIAL; autoincrement; pkey.                                                               |
| `tenant_id`   | string | owner scope.                                                                                  |
| `entity_id`   | string | to whom the policy is applied                                                                 |
| `scope`       | string |                                                                                               |
| `code`        | string | policy's name                                                                                 |
| `applied_js`  | string | optional; defaults to true. `function applies(entity, ctx)`                                   |
| `evaluate_js` | string | `function evaluate(input, entity, ctx)` returns a boolean (true = allow) or `{allow, reason}` |
| `status`      | enum   | ACTIVE; INACTIVE; EXPIRED                                                                     |
| `valid_from`  | bigint | epoch time                                                                                    |
| `valid_till`  | bigint | epoch time                                                                                    |
| `version`     | int    | incremented on every update                                                                   |
| `created_at`  | bigint | epoch time                                                                                    |
| `updated_at`  | bigint | epoch time                                                                                    |

- **constraints**
    - primary key (id)
    - index (code)
    - index (entity_id)

## policy_evaluations - audit

| field         | type      | notes                                                                       |
|---------------|-----------|-----------------------------------------------------------------------------|
| `id`          | bigserial |                                                                             |
| `request_id`  | string    | an evaluation id returned along with the evaluate response for traceability. |
| `policy_id`   | bigint    |                                                                             |
| `input_attrs` | jsonb     | the input passed                                                            |
| `result`      | enum      | DENY; ALLOW;                                                                |
| `error`       | jsonb     | dumps of errors if any occured                                              |
| `created_at`  | bigint    | epoch time                                                                  |
| `modified_at` | bigint    | epoch time                                                                  |

# policy evaluation

an "entity" could have two types policies that can be applied to him. one is the entity level policy which the clients
could create. apart from from that there are also scope level policies.

```sql
select 
  *
from 
  policies
where
  entity_id in ('*', ':entity_id')
  and status = 'ACTIVE'
  and scope = 'payment.credit'
  and tenant_id = 1;
```

from the list of policies we receive, we will evaluate everyone of them, and see if metrics of each of them. if any of
the evaluation fails, we will return `deny`  and a list of policies which were violated.

## execution

to execute the JS functions, we will use GraalJS to run the JS functions. all of them run in parallel in multiple
threads, and the futures are accumulated and validated if any of the completed with a failure, if yes, then we will
capture that and return that to the user.

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>dev.libra</groupId>
  <artifactId>graaljs-demo</artifactId>
  <version>0.1.0</version>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <graaljs.version>25.2.4</graaljs.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.graalvm.polyglot</groupId>
      <artifactId>polyglot</artifactId>
      <version>${graaljs.version}</version>
    </dependency>
    <dependency>
      <groupId>org.graalvm.polyglot</groupId>
      <artifactId>js-community</artifactId>
      <version>${graaljs.version}</version>
      <type>pom</type>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.5.0</version>
        <configuration>
          <mainClass>dev.libra.GraalJsDemo</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

```
package dev.libra;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class GraalJsDemo {
  
    private static final Engine ENGINE = Engine.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false") // silence the warning on stock OpenJDK
            .build();

    private static final ScheduledExecutorService WATCHDOG =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "js-watchdog");
                t.setDaemon(true);
                return t;
            });

    private static final ResourceLimits LIMITS = ResourceLimits.newBuilder()
            .statementLimit(1_000_000, null) // null = applies to all sources
            .build();

    private static final long TIMEOUT_MS = 50;
    
    static String run(String name, String jsCode, String inputJson) {
        Source source = Source.newBuilder("js", jsCode, name + ".js").buildLiteral();

        try (Context ctx = Context.newBuilder("js")
                .engine(ENGINE)
                .allowAllAccess(false)      // no Java classes, files, network, threads
                .resourceLimits(LIMITS)
                .build()) {

            ScheduledFuture<?> kill =
                    WATCHDOG.schedule(() -> ctx.close(true), TIMEOUT_MS, TimeUnit.MILLISECONDS);
            try {
                ctx.eval(source);

                Value evaluate = ctx.getBindings("js").getMember("evaluate");
                if (evaluate == null || !evaluate.canExecute()) {
                    return "ERROR: no evaluate() function";
                }

                // Pass input as JSON and parse it inside JS: the policy gets a plain copy.
                Value input = ctx.eval("js", "JSON.parse").execute(inputJson);
                Value result = evaluate.execute(input);

                if (!result.isBoolean()) {
                    return "ERROR: evaluate() must return a boolean";
                }
                return result.asBoolean() ? "ALLOW" : "DENY";

            } catch (PolyglotException e) {
                if (e.isCancelled())         return "ERROR: timeout";
                if (e.isResourceExhausted()) return "ERROR: statement limit exceeded";
                return "ERROR: " + e.getMessage();
            } finally {
                kill.cancel(false);
            }
        }
    }

    public static void main(String[] args) {
        String input = """
                { "user": "account_holder_1", "channel": "sms", "hour": 23 }
                """;

        // 1. A normal policy: no SMS after 22:00.
        System.out.println("quiet-hours  -> " + run("quiet-hours", """
                function evaluate(input) {
                  return !(input.channel === "sms" && input.hour >= 22);
                }
                """, input));

        // 2. Infinite loop: stopped by the statement limit or the watchdog.
        System.out.println("busy-loop    -> " + run("busy-loop", """
                function evaluate(input) { while (true) {} }
                """, input));

        // 3. Tries to reach Java: blocked by allowAllAccess(false).
        System.out.println("host-access  -> " + run("host-access", """
                function evaluate(input) { return Java.type("java.lang.System") !== null; }
                """, input));

        // 4. Wrong return type.
        System.out.println("bad-return   -> " + run("bad-return", """
                function evaluate(input) { return "yes"; }
                """, input));

        // 5. Throws.
        System.out.println("throws       -> " + run("throws", """
                function evaluate(input) { throw new Error("boom"); }
                """, input));

        ENGINE.close();
    }
}
```

# sweep

```
apiVersion: batch/v1
kind: CronJob
metadata: { name: libra-sweeper }
spec:
  schedule: "0 * * * *"
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: sweep
              image: curlimages/curl
              env:
                - name: TOKEN
                  valueFrom: { secretKeyRef: { name: libra-internal, key: token } }
              args: [ "-fsS", "-X", "POST", "-H", "Authorization: Bearer $(TOKEN)", "http://libra-server/internal/sweep" ]
```

# api

| <u>**Method**</u>                    | <u>**Path**</u>                                            | <u>**Purpose**</u>                                                                                                                                        |
|--------------------------------------|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `<u>POST</u>`                        | `<u>/v1/tenants</u>`                                       | <u>Create a tenant</u>                                                                                                                                    |
| `<u>POST</u>`                        | `<u>/v1/tenants/{t}/scopes</u>`                            | <u>Create a scope</u>                                                                                                                                     |
| `<u>GET</u>`<u> / </u>`<u>PATCH</u>` | `<u>/v1/tenants/{t}/scopes/{s}</u>`                        | <u>Read or update a scope</u>                                                                                                                             |
| `<u>POST</u>`                        | `<u>/v1/tenants/{t}/scopes/{s}/policies</u>`               | <u>Create a policy with its </u>`<u>identifier</u>`<u> (</u>`<u>*</u>`<u> or an entity ID). Compiles the code and runs the optional </u>`<u>examples</u>` |
| `<u>PUT</u>`                         | `<u>/v1/tenants/{t}/scopes/{s}/policies/by-key/{key}</u>`  | <u>Idempotent upsert by external key (used by the operator)</u>                                                                                           |
| `<u>GET</u>`                         | `<u>/v1/tenants/{t}/scopes/{s}/policies</u>`               | <u>List policies, filterable by </u>`<u>identifier</u>`<u> and </u>`<u>status</u>`                                                                        |
| `<u>GET</u>`<u> / </u>`<u>PATCH</u>` | `<u>/v1/tenants/{t}/policies/{id}</u>`                     | <u>Read or update a policy. </u>`<u>PATCH</u>`<u> bumps </u>`<u>version</u>`                                                                              |
| `<u>POST</u>`                        | `<u>/v1/tenants/{t}/policies/{id}/deactivate</u>`          | <u>Soft disable</u>                                                                                                                                       |
| `<u>POST</u>`                        | `<u>/v1/tenants/{t}/policies/{id}/reactivate</u>`          | <u>Restore an inactive or expired policy</u>                                                                                                              |
| `<u>POST</u>`                        | `<u>/v1/tenants/{t}/policies/{id}/test</u>`                | <u>Dry-run against a sample entity and input. Nothing is stored</u>                                                                                       |
| `<u>POST</u>`                        | `<u>/v1/tenants/{t}/scopes/{s}/evaluate</u>`               | <u>Get a decision</u>                                                                                                                                     |
| `<u>POST</u>`                        | `<u>/internal/sweep</u>`                                   | <u>Mark expired policies (internal only)</u>                                                                                                              |
| `<u>GET</u>`                         | `<u>/healthz</u>`<u>, </u>`<u>/metrics</u>`                | <u>Liveness and Prometheus metrics</u>                                                                                                                    |

## api specs

all endpoints take and return `application/json`. timestamps are epoch milliseconds. `{t}` is the tenant id, `{s}` the
scope code, `{id}` the policy id. callers are trusted, so there is no auth on public endpoints; `/internal/sweep`
requires `Authorization: Bearer <token>`.

**error format** (every 4xx / 5xx)

```
{
  "error": {
    "code": "POLICY_COMPILE_ERROR",
    "message": "SyntaxError: Unexpected token at 2:14",
    "details": { "field": "evaluate_js", "line": 2, "column": 14 }
  }
}
```

| status | when                                                                                     |
|--------|------------------------------------------------------------------------------------------|
| 400    | invalid body, js that doesn't compile, `entity.id = "*"` on evaluate, failing `examples` |
| 404    | tenant, scope or policy not found                                                        |
| 409    | duplicate `code`, or `version` mismatch on update                                        |
| 413    | body over 64 KB                                                                          |
| 500    | unexpected server error                                                                  |

### POST /v1/tenants

create a tenant.

request

```
{ "tenant_code": "acme", "tenant_name": "Acme Corp" }
```

response `201`

```
{ "id": 1, "tenant_code": "acme", "tenant_name": "Acme Corp", "created_at": 1793900000000 }
```

errors: `400` missing fields, `409` tenant_code already exists.

### POST /v1/tenants/{t}/scopes

create a scope within a tenant.

request

```
{ "code": "notification", "description": "outbound notifications" }
```

response `201`

```
{ "tenant_id": 1, "code": "notification", "description": "outbound notifications", "created_at": 1793900000000 }
```

errors: `404` tenant, `409` scope code already exists in this tenant.

### GET /v1/tenants/{t}/scopes/{s}

response `200`

```
{ "tenant_id": 1, "code": "notification", "description": "outbound notifications", "policy_count": 12, "created_at": 1793900000000, "updated_at": 1793900000000 }
```

### PATCH /v1/tenants/{t}/scopes/{s}

request (any subset)

```
{ "description": "sms, email and push notifications" }
```

response `200`: the updated scope.

### POST /v1/tenants/{t}/scopes/{s}/policies

create a policy. both functions are compiled before saving; if `examples` are given, each must produce the expected
result or the policy is rejected.

request

```
{
  "code": "quiet-hours",
  "entity_id": "*",
  "applied_js": "function applies(entity, ctx) { return entity.attributes.region === 'eu'; }",
  "evaluate_js": "function evaluate(input, entity, ctx) { const h = new Date(ctx.now).getUTCHours(); return { allow: h >= 8 && h < 22, reason: 'quiet hours' }; }",
  "valid_from": 1793900000000,
  "valid_till": 1796500000000,
  "examples": [
    { "entity": { "id": "account-holder-1", "attributes": { "region": "eu" } }, "input": { "channel": "sms" }, "now": 1793955600000, "expect": "DENY" }
  ]
}
```

| field         | required | notes                                                |
|---------------|----------|------------------------------------------------------|
| `code`        | yes      | policy name, unique per tenant, scope and entity_id  |
| `entity_id`   | yes      | `*` for the whole scope, or one entity id            |
| `applied_js`  | no       | defaults to always applies                           |
| `evaluate_js` | yes      |                                                      |
| `valid_from`  | no       | defaults to now                                      |
| `valid_till`  | no       | null = never expires                                 |
| `examples`    | no       | not stored; only used to validate the policy         |

response `201`

```
{
  "id": 42, "tenant_id": 1, "scope": "notification", "entity_id": "*", "code": "quiet-hours",
  "status": "ACTIVE", "effective_status": "ACTIVE",
  "valid_from": 1793900000000, "valid_till": 1796500000000,
  "version": 1, "created_at": 1793900000000, "updated_at": 1793900000000
}
```

errors: `400` compile error or failed example, `404` tenant or scope, `409` duplicate code.

### PUT /v1/tenants/{t}/scopes/{s}/policies/by-key/{key}

idempotent create-or-update used by the k8s operator. `{key}` is the policy `code`; `entity_id` comes from the body.
same body as create, plus an optional `generation`; if libra has already applied that generation the call is a no-op.

request

```
{ "entity_id": "*", "evaluate_js": "...", "valid_till": 1796500000000, "generation": 3 }
```

response `200` updated or unchanged, `201` created; body is the policy.

### GET /v1/tenants/{t}/scopes/{s}/policies

list policies in a scope.

query params
| param | notes |
| ----- | ----- |
| `entity_id` | `*` or an entity id |
| `status` | `ACTIVE`, `INACTIVE`, `EXPIRED` (matched against `effective_status`) |
| `limit` | default 50, max 200 |
| `cursor` | from the previous page's `next_cursor` |

response `200`

```
{ "items": [ { "id": 42, "code": "quiet-hours", "entity_id": "*", "status": "ACTIVE", "effective_status": "EXPIRED", "version": 3, "valid_till": 1793000000000 } ], "next_cursor": null }
```

`effective_status` is computed on read: `EXPIRED` once `now >= valid_till`, even if the sweeper hasn't run yet.

### GET /v1/tenants/{t}/policies/{id}

response `200`: the full policy, including `applied_js` and `evaluate_js`.

### PATCH /v1/tenants/{t}/policies/{id}

update a policy. `version` is required for optimistic locking; changed code is recompiled. a successful update
increments `version`.

request

```
{ "version": 3, "evaluate_js": "function evaluate(input) { return input.channel !== 'sms'; }", "valid_till": 1799000000000 }
```

response `200`: the updated policy with `version: 4`.
errors: `400` compile error, `404`, `409` version mismatch (someone else updated it first).
note: moving `valid_till` forward on an `EXPIRED` policy does not reactivate it; use `/reactivate`.

### POST /v1/tenants/{t}/policies/{id}/deactivate

soft disable; sets `status = INACTIVE`. no body.
response `200`: the policy. calling it on an inactive policy is a no-op.

### POST /v1/tenants/{t}/policies/{id}/reactivate

sets `status = ACTIVE` for an `INACTIVE` or `EXPIRED` policy.

request (optional, needed when the policy has already expired)

```
{ "valid_till": 1799000000000 }
```

response `200`: the policy.
errors: `400` if `valid_till` would still be in the past.

### POST /v1/tenants/{t}/policies/{id}/test

dry-run one policy. nothing is stored and no policy_evaluations row is written.

request

```
{ "entity": { "id": "account-holder-1", "attributes": { "region": "eu" } }, "input": { "channel": "sms" }, "now": 1793955600000 }
```

`now` is optional and lets you test time-based rules.

response `200`

```
{ "applies": true, "result": "DENY", "reason": "quiet hours", "error": null, "duration_ms": 3 }
```

### POST /v1/tenants/{t}/scopes/{s}/evaluate

get a decision. libra loads the entity's own policies plus the scope's `*` policies, keeps those that are live and whose
`applies()` returns true, and runs `evaluate()` on each. the result is `ALLOW` only if at least one policy applied and
all of them allowed; any deny or error gives `DENY`.

request

```
{
  "entity": { "id": "account-holder-1", "attributes": { "region": "eu" } },
  "input": { "channel": "sms", "template": "promo" }
}
```

response `200`

```
{
  "request_id": "exe_01HXYZ9K2M",
  "decision": "DENY",
  "evaluated": 3,
  "violations": [
    { "policy_id": 42, "code": "quiet-hours", "entity_id": "*", "result": "DENY", "reason": "quiet hours" },
    { "policy_id": 57, "code": "promo-limit", "entity_id": "account-holder-1", "result": "ERROR", "error": "timeout" }
  ],
  "trace": [
    { "policy_id": 42, "version": 3, "applies": true, "result": "DENY", "reason": "quiet hours", "duration_ms": 2 },
    { "policy_id": 51, "version": 1, "applies": true, "result": "ALLOW", "duration_ms": 1 },
    { "policy_id": 57, "version": 2, "applies": true, "result": "ERROR", "error": "timeout", "duration_ms": 50 },
    { "policy_id": 60, "version": 1, "applies": false }
  ],
  "valid_until": 1796500000000
}
```

| field         | notes                                                                                    |
|---------------|------------------------------------------------------------------------------------------|
| `request_id`  | id of the rows written to `policy_evaluations`, for audit lookups                        |
| `evaluated`   | number of policies that applied                                                          |
| `violations`  | every policy that denied or errored; empty on `ALLOW`                                    |
| `trace`       | every candidate policy, including ones that didn't apply                                 |
| `valid_until` | earliest `valid_till` among applied policies; a client may cache the decision until then |

if no policy applies, the response is `DENY` with `evaluated: 0` and an empty `violations` list.
errors: `400` missing entity or `entity.id = "*"`, `404` tenant or scope, `413` body too large.

### POST /internal/sweep

marks every `ACTIVE` policy whose `valid_till` has passed as `EXPIRED` and bumps its `version`. called hourly by the
cron job; safe to call any time. not exposed through the public ingress.

headers: `Authorization: Bearer <token>`
response `200`

```
{ "expired": 7, "ran_at": 1793908800000 }
```

errors: `401` missing or wrong token.

### GET /healthz

response `200` `{ "status": "ok" }` when the service can reach the database; `503` otherwise.

### GET /metrics

prometheus text format. includes `libra_decisions_total{tenant,scope,decision}`, `libra_policy_errors_total{policy_id}`
and `libra_eval_seconds`.