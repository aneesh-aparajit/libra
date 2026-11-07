# libra

a small, multi-tenant policy evaluation engine. send an entity and an input, get back `allow` or `deny`, decided by
policies written as plain javascript functions.

> **status:** early work in progress. storage, the js engine, and the tenant / scope / policy http api are in place;
> the evaluation endpoint is not yet.

## how it works

- **tenant**: an organization, and the boundary every call is scoped within.
- **scope**: a group of users within a tenant, e.g. `notification`.
- **entity**: an opaque identifier supplied by the client.
- **policy**: a pair of js scripts, targeted at a whole scope (`*`) or at one entity. each script must define a
  function named `evaluate`.

```js
// applied_js: does this policy apply? optional, defaults to true
function evaluate(entity, ctx) {
    return entity.attributes.region === "eu";
}
```

```js
// evaluate_js: allow or deny. true / false, or { allow, reason }
function evaluate(input, entity, ctx) {
    const hour = new Date(ctx.now).getUTCHours();
    return {allow: hour >= 8 && hour < 22, reason: "quiet hours"};
}
```

all applicable policies run in parallel on graaljs. every one must allow; any deny or error means `deny`. each
evaluation is recorded in `policy_evaluations` for auditing.

## stack

java 21 · spring boot 4 · postgresql · redis · graaljs · gson

## layout

| module       | what's in it                                                                        |
|--------------|-------------------------------------------------------------------------------------|
| `libra-core` | domain entities, DAOs and row mappers, services, js engine, jdbc/json utils, schema |
| `libra-api`  | spring boot application: controllers, request/response dtos, transformers, errors   |
| `docs/`      | [design doc](docs/libra.md)                                                         |

## running locally

start postgres and redis, build, and run the server on `:8080`:

```bash
docker compose up -d postgres redis
```

```bash
mvn clean install
```

```bash
java -jar libra-api/target/libra-api-1.0-SNAPSHOT.jar
```

### configuration

| variable               | default                                  |
|------------------------|------------------------------------------|
| `LIBRA_DB_URL`         | `jdbc:postgresql://localhost:5432/libra` |
| `LIBRA_DB_USERNAME`    | `postgres`                               |
| `LIBRA_DB_PASSWORD`    | `postgres`                               |
| `LIBRA_REDIS_HOST`     | `localhost`                              |
| `LIBRA_REDIS_PORT`     | `6379`                                   |
| `LIBRA_REDIS_PASSWORD` | _(empty)_                                |

defaults match `docker-compose.yaml`.

### schema

the schema lives in `libra-core/src/main/resources/db/schema.sql`. there are no migrations: docker compose applies it
when the postgres volume is first created, and the app never runs it. for an existing database, apply it by hand (once;
it is not idempotent):

```bash
psql -h localhost -U postgres -d libra -f libra-core/src/main/resources/db/schema.sql
```

## api

all endpoints are json. creates return `201` with a `Location` header.

| method  | path                       | notes                                                                        |
|---------|----------------------------|------------------------------------------------------------------------------|
| `POST`  | `/v1/tenants`              | `{tenantCode, tenantName}`                                                   |
| `GET`   | `/v1/tenants/{id}`         |                                                                              |
| `GET`   | `/v1/tenants?code=`        | lookup by tenant code                                                        |
| `POST`  | `/v1/scopes`               | `{tenantID, scopeName}`                                                      |
| `GET`   | `/v1/scopes/{id}`          |                                                                              |
| `GET`   | `/v1/scopes?tenantID=`     | all scopes of a tenant                                                       |
| `POST`  | `/v1/policies`             | `status` defaults to `ACTIVE`; `validTo` optional, must be after `validFrom` |
| `GET`   | `/v1/policies/{id}`        |                                                                              |
| `GET`   | `/v1/policies`             | filter by `tenantID`+`scopeID`, `entityID`+`scopeID`, or `entityID`          |
| `GET`   | `/v1/policies/applicable`  | `tenantID`, `scopeID`, `entityID`, optional `at` (epoch ms, default now)     |
| `PUT`   | `/v1/policies/{id}`        | full update; send the current `version` (optimistic locking)                 |
| `PATCH` | `/v1/policies/{id}/status` | `{status}`; expired policies cannot change status                            |
| `GET`   | `/healthz`                 | postgres + redis health                                                      |

```bash
curl -s -X POST localhost:8080/v1/tenants -H 'content-type: application/json' \
  -d '{"tenantCode": "acme", "tenantName": "Acme Corp"}'
```

errors are [rfc 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "tenant 9 not found",
  "instance": "/v1/tenants/9"
}
```

| status | when                                                                              |
|--------|-----------------------------------------------------------------------------------|
| `400`  | validation failure, malformed body or params, unknown referenced tenant/scope     |
| `404`  | resource not found                                                                |
| `409`  | duplicate resource, stale `version` on update, status change on an expired policy |
| `500`  | anything else; details are logged, never returned                                 |

### health

`GET /healthz` aggregates `db` (postgres), `redis`, disk space and ping. it returns `200` when everything is `UP` and
`503` when any component is `DOWN`. only component names and statuses are shown, no hosts or versions.

## testing

docker must be running: the DAO tests start postgres with testcontainers.

```bash
mvn test
```

run one module, or one class:

```bash
mvn test -pl libra-api -am
```

```bash
mvn test -pl libra-api -am -Dtest=PolicyControllerTest -Dsurefire.failIfNoSpecifiedTests=false
```

| module       | tests                                                                            |
|--------------|----------------------------------------------------------------------------------|
| `libra-core` | DAOs against real postgres, row mappers, services, graaljs engine                |
| `libra-api`  | controllers (`@WebMvcTest`, mocked services) and transformers (plain unit tests) |
