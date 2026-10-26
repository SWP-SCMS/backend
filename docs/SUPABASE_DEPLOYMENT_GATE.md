# Supabase grants and RLS deployment gate

This gate is separate from application releases. Do not change production grants or RLS until every staging item below is complete and its evidence is attached to the deployment record.

## 1. Capture the current state

- Export grants for `anon`, `authenticated`, and the backend database role from staging.
- Export enabled RLS flags and policies for every application table and sequence.
- Record the frontend Supabase URL/key configuration and verify whether the frontend performs any direct table access.
- Store the snapshot in the restricted deployment record; do not commit credentials or keys.

## 2. Design the least-privilege change

- Revoke direct table and sequence access from `anon` and `authenticated` unless a documented frontend flow requires it.
- Keep the backend role permissions required by Flyway and the application.
- Add RLS policies only for approved direct-access flows; default to no direct frontend access.
- Prepare a forward migration and a rollback script from the captured staging state. Never edit an applied migration.

## 3. Verify on staging

- Restore a fresh database and run every current Flyway migration.
- Upgrade a copy of the current staging schema and data.
- Apply the grants/RLS change, then run unit tests and all four Testcontainers shards.
- Run API smoke tests for authentication, membership/payment, scheduling, coaching/training, audit, notification, and reporting.
- Confirm `anon` and `authenticated` cannot read or mutate tables outside explicitly approved policies.
- Confirm the backend role can migrate, start, and complete all smoke flows.
- Execute the rollback script and verify that the captured grants and policies are restored.

## 4. Production rollout

- Obtain approval from the database owner and application owner using the staging evidence.
- Take a fresh production grants/RLS snapshot immediately before deployment.
- Apply the reviewed migration during the approved deployment window.
- Run read-only health checks first, followed by the critical API smoke tests.
- Roll back immediately if authorization changes, migrations fail, or any critical API returns an unexpected error.

## Required evidence

- Before/after grants and RLS snapshots.
- Forward migration and rollback script hashes.
- Fresh-database and upgrade-database results.
- API regression and security test results.
- Approvers, deployment time, operator, and rollback decision.
