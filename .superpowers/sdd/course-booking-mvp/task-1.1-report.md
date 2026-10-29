# Task 1.1 report — Course catalog schema and state foundation

## Result

Implementation commit: `8662ad0ea612d159300022a5932e2fdc5fbbb348`

Task 1.1 adds only the Course, CourseRun, and CourseSession database/JPA foundation. It does not add controllers, Booking integration, cross-domain availability, commerce, schedulers, notifications, or frontend code.

## Changed files

- `src/main/resources/db/migration/V51__course_catalog.sql`
  - Creates `courses`, `course_runs`, and `course_sessions`.
  - Adds required foreign keys, status/value checks, chronology checks, unique codes and run/unit identity, replacement self-reference protection, and lookup indexes.
  - Uses explicit VND amount columns without a redundant currency column.
- `src/main/java/com/scms/backend/course/Course.java`
- `src/main/java/com/scms/backend/course/CourseRun.java`
- `src/main/java/com/scms/backend/course/CourseSession.java`
- `src/main/java/com/scms/backend/course/CourseStatus.java`
- `src/main/java/com/scms/backend/course/CourseRunStatus.java`
- `src/main/java/com/scms/backend/course/CourseSessionStatus.java`
- `src/main/java/com/scms/backend/course/CourseException.java`
  - Adds JPA mappings, optimistic versions, package-scoped CourseRun transitions, immutable opening validation/policy snapshotting, and stable error codes.
- `src/test/java/com/scms/backend/course/CourseRunTests.java`
  - Covers the required lifecycle, cancellable states, terminal/skipped transitions, opening validation, policy snapshot, and DRAFT-only minimum enrollment changes.
- `src/test/java/com/scms/backend/course/CourseCatalogMigrationV51Tests.java`
  - Testcontainers fresh-schema contract for valid inserts and representative course/run/session constraints.
- `src/test/java/com/scms/backend/FoundationIntegrationTests.java`
  - Extends the expected Flyway history through V51.

## TDD evidence

RED command, run before production code:

```text
.\mvnw.cmd -ntp -Dtest=CourseRunTests test
```

Expected RED observed: Maven test compilation failed with 29 `cannot find symbol` errors for the intentionally missing `CourseRun`, `CourseRunStatus`, and `CourseException` types. Exit code 1.

GREEN command after the minimal implementation:

```text
.\mvnw.cmd -ntp -Dtest=CourseRunTests test
```

Result: 16 tests, 0 failures, 0 errors, exit code 0. The final pre-commit rerun produced the same result.

## Validation

- Focused state tests: 16 tests, 0 failures, 0 errors.
- CI-equivalent non-container suite: 168 classes; 822 tests, 0 failures, 0 errors, exit code 0.
- V51 migration contract attempt: 1 test, 0 failures, 1 environment error because Testcontainers could not find a valid Docker environment. Test compilation succeeded.
- Full Maven attempt: 899 tests, 0 failures, 77 errors; all 77 are Docker/Testcontainers environment errors, including the new V51 contract test.
- `git diff --cached --check`: clean before the implementation commit.
- `git fetch origin main`: `origin/main` remained `13ea688`; V50 was still the latest applied migration, so V51 remained the next free version.
- Pre-report status contained only the pre-existing untracked `.superpowers/sdd/course-booking-mvp/` directory; `task-1.1-brief.md` and `progress.md` were not staged or modified.

## Self-review

- The SQL and JPA mappings contain every field and status listed in the brief.
- Positive amount/enrollment/unit constraints, enrollment-to-capacity, run chronology, session chronology, unique run/unit, and non-self replacement are enforced in SQL.
- CourseRun transition methods are package-scoped and match the required graph exactly; `SETTLEMENT_PENDING` cannot cancel, and terminal states reject all tested transitions.
- Opening registration validates immutable pricing/enrollment/schedule details before mutating state and snapshots a non-blank policy version.
- `minimumEnrollment` is mutable only while DRAFT and remains unchanged after a rejected mutation.
- Existing migrations V1–V50 and unrelated domain code were not changed.

## Concerns

- Docker is unavailable locally, so PostgreSQL execution of V51 and Hibernate schema validation were not observed here. The focused contract compiles and is ready for the Docker-enabled CI integration shard.
- The report is committed separately so it can record the exact implementation commit SHA without a self-referential commit hash.

## Review fixes — 2026-10-29

- Updated `chk_course_runs_policy_snapshot` so `DRAFT` and `CANCELLED` accept a null or nonblank snapshot, while every opened operational state requires a nonblank snapshot.
- Added a PostgreSQL contract case covering successful `DRAFT -> CANCELLED` with a null snapshot and rejection of `DRAFT -> OPEN_FOR_REGISTRATION` with a null snapshot.
- Aligned opening validation with the database columns: tuition accepts 19 digits and rejects 20; policy version accepts 100 characters and rejects 101 with `COURSE_RUN_INVALID_OPENING`.

Review-fix TDD and validation evidence:

- RED: `.\mvnw.cmd -ntp -Dtest=CourseRunTests test` ran 19 tests with exactly 2 expected failures and 0 errors; the missing guards accepted 20-digit tuition and a 101-character policy.
- GREEN: the same focused command ran 19 tests with 0 failures and 0 errors.
- Non-container CI-equivalent suite: 168 classes; 825 tests, 0 failures, 0 errors.
- PostgreSQL contract command was attempted before and after the SQL fix. Both attempts compiled the tests, then stopped before execution because Testcontainers could not find a valid Docker environment.
- `git diff --check`: clean (line-ending conversion warnings only).
- The remaining local concern is unchanged: the new PostgreSQL assertions require Docker-enabled CI for execution.

## CI migration-head follow-up — 2026-10-29

- CI Push/PR integration shard 0 supplied RED evidence at `292d2d3`: `MembershipOrderMigrationV7Tests` line 56 expected Flyway history only through V50, while the fresh migration correctly applied V51. CI also confirmed V51 fresh migration and Hibernate startup succeeded; this was the only failed assertion.
- Updated that exact full-history assertion to include V51. A repository search found only one other exact migration-head assertion, `FoundationIntegrationTests`, which already includes V51. Migration-specific checks for V39/V49 were intentionally unchanged.
- Local compile: `.\mvnw.cmd -ntp -DskipTests test` succeeded.
- Local focused migration test compiled then stopped before execution because Testcontainers could not find a valid Docker environment.
- CI-equivalent non-container suite: 168 classes; 825 tests, 0 failures, 0 errors.
