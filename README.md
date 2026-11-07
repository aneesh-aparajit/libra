# libra

a small, multi-tenant policy evaluation engine. send an entity and an input, get back `allow` or `deny`, decided by
policies written as plain javascript functions.

> **status:** early work in progress. the data model and storage layer are in place; evaluation and the api are not yet.

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
    return { allow: hour >= 8 && hour < 22, reason: "quiet hours" };
}
```

all applicable policies run in parallel on graaljs. every one must allow; any deny or error means `deny`. each
evaluation is recorded in `policy_evaluations` for auditing.

## stack

java 21 · spring boot 4 · postgresql · graaljs · gson

## layout

| module       | what's in it                                                       |
|--------------|--------------------------------------------------------------------|
| `libra-core` | domain entities, DAOs and row mappers, jdbc/json utils, js engine  |
| `libra-api`  | spring boot application and http layer                             |
| `docs/`      | [design doc](docs/libra.md)                                        |

## running locally

```bash
docker compose up -d postgres
```

```bash
mvn clean install
```

the app reads `LIBRA_DB_URL`, `LIBRA_DB_USERNAME`, `LIBRA_DB_PASSWORD`, `LIBRA_REDIS_HOST`, `LIBRA_REDIS_PORT` and
`LIBRA_REDIS_PASSWORD` (defaults match `docker-compose.yaml`). `GET /healthz` reports postgres and redis health and
returns 503 if either is down.
the schema lives in `libra-core/src/main/resources/db/schema.sql`; docker compose applies it when the postgres volume
is first created. for an existing database, apply it by hand:

```bash
psql -h localhost -U postgres -d libra -f libra-core/src/main/resources/db/schema.sql
```
