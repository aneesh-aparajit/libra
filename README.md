# libra

a small, multi-tenant policy evaluation engine. send an entity and an input, get back `allow` or `deny`, decided by
policies written as plain javascript functions.

> **status:** early work in progress. the data model and storage layer are in place; evaluation and the api are not yet.

## how it works

- **tenant**: an organization, and the boundary every call is scoped within.
- **scope**: a group of users within a tenant, e.g. `notification`.
- **entity**: an opaque identifier supplied by the client.
- **policy**: a pair of js functions, targeted at a whole scope (`*`) or at one entity.

```js
function applies(entity, ctx) {          // optional, defaults to true
    return entity.attributes.region === "eu";
}

function evaluate(input, entity, ctx) {   // true / false, or { allow, reason }
    const hour = new Date(ctx.now).getUTCHours();
    return { allow: hour >= 8 && hour < 22, reason: "quiet hours" };
}
```

all applicable policies run in parallel on graaljs. every one must allow; any deny or error means `deny`. each
evaluation is recorded in `policy_evaluations` for auditing.

## stack

java 21 · spring boot 4 · postgresql + flyway · graaljs · gson

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

the app reads `LIBRA_DB_URL`, `LIBRA_DB_USERNAME` and `LIBRA_DB_PASSWORD` (defaults match `docker-compose.yaml`), and
flyway applies the schema on startup.
