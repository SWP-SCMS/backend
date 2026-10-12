# US51 Notification Center

## Files changed

- Added owner-scoped notification repository queries, service, controller, DTOs, and notification-specific exceptions.
- Added `NOTIFICATION_NOT_FOUND` and pagination validation problem responses.
- Secured `/notifications/**` for authenticated accounts and documented all three routes through controller annotations.
- Updated generated OpenAPI route expectations.
- Added service, MockMvc controller, and Testcontainers integration tests.

## Validation

- `mvnw.cmd -q '-Dtest=NotificationServiceTests,NotificationControllerTests' test`: passed (5 tests).
- `mvnw.cmd -q -Dtest=GeneratedOpenApiTests test`: passed.
- `mvnw.cmd -q -DskipTests package`: passed.
- `mvnw.cmd -q -Dtest=NotificationCenterIntegrationTests test`: not runnable in this environment because Docker is unavailable; the test is retained for Docker-enabled CI.

## Concerns

The integration test requires a running Docker/Testcontainers environment. Notification payload is returned as the existing structured payload; delivery-attempt/status internals and recipient account identifiers are excluded.
