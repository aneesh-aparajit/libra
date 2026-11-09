# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Service engineers integrating their own service with libra: they call `POST /v1/evaluate` from their code and write
the javascript policies that decide allow or deny for their tenant and scope.

## Product Purpose

libra is a small, multi-tenant policy evaluation engine. A service sends an entity and an input; libra runs every
applicable policy and returns `ALLOW` or `DENY` with a per-policy trace. It exists so authorization and business-rule
checks stop being hard-coded across services: rules change without a redeploy, every decision is audited, and
temporary rules expire on their own.

The documentation site's first job is to make a service engineer understand what libra is and why it exists; the
policy contract and api reference follow.

## Positioning

Policies are plain javascript functions (`function evaluate(input)`), not rego or another declarative language. Each
policy is a pair of scripts (an applicability check and the decision), all applicable policies run in parallel in a
sandboxed graaljs context, and the rule is unanimous: every policy must allow, any deny or error denies.

## Operating Context

- Callers are trusted services; libra has no auth layer of its own (non-goal).
- Entities are opaque identifiers; libra stores no entity data or scope membership.
- Policies target a whole scope (`*`) or a single entity, with `validFrom` / `validTo` windows.
- Every evaluation is recorded in `policy_evaluations` under a returned `requestID`.
- Stack: java 21, spring boot 4, postgresql, redis, graaljs, gson.

## Capabilities and Constraints

- HTTP api: tenants, scopes, policies (create, get, list, applicable, update with optimistic locking, status patch),
  evaluate, healthz. Errors are rfc 9457 problem details.
- Status: early work in progress. A k8s crd and an expiry sweeper are planned in the design doc, not built.
- The docs site is hosted on GitHub Pages from `site/` via `.github/workflows/pages.yml`.

## Brand Commitments

- Lowercase voice, matching the README.
- The docs site is a single page with anchors.
- The docs site ships no javascript: pure HTML and CSS.

## Evidence on Hand

- `README.md`, `docs/libra.md` (design doc), and the request/response records in
  `libra-api/src/main/java/dev/tsukuyomi/libra/dto/`.
- No users, adopters, benchmarks, or performance numbers exist; never fabricate them.

## Product Principles

- Decide in one place, enforce everywhere: services ask, libra decides.
- Unanimity is the safe default: any deny or error denies.
- Every decision is explainable after the fact.
- Policies are ordinary code an engineer already knows how to write.
