# libra: prd

libra is a small, multi-tenant policy evaluation engine. a client sends an entity and an input, and libra returns allow
or
deny based on policies written as JavaScript functions.

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

| notion | what it is                                                                                                |
|--------|-----------------------------------------------------------------------------------------------------------|
| tenant | an organization, and the isolation boundary, every call is scoped within.                                 |
| scope  | a group of users within a tenant, e.g. `notification`                                                     |
| entity | the identifier for the user, supplied by the client. libra doesn't care what it signifies for the client. |
| policy | a rule targeted at a whole scope or at one entity.                                                        |

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

| field         | type      | notes                                                                        |
|---------------|-----------|------------------------------------------------------------------------------|
| `id`          | bigserial |                                                                              |
| `request_id`  | string    | an evaluation id returned along with the evaluate response for traceability. |
| `policy_id`   | bigint    |                                                                              |
| `input_attrs` | jsonb     | the input passed                                                             |
| `result`      | enum      | DENY; ALLOW;                                                                 |
| `error`       | jsonb     | dumps of errors if any occured                                               |
| `created_at`  | bigint    | epoch time                                                                   |
| `modified_at` | bigint    | epoch time                                                                   |

# policy evaluation

an "entity" could have two types policies that can be applied to him. one is the entity level policy which the clients
could create. apart from from that there are also scope level policies.

```sql
select *
from policies
where entity_id in ('*', ':entity_id')
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

```java
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
