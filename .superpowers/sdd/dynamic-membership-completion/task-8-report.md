# Progress 8 report — legacy Yoga handling

## Result

Merged in PR #89 with squash commit `03df03418b401285743a65504a0f2484aa27eac9`.

New Yoga data is blocked at the scheduling mutation boundaries:

- Discipline creation and rename to `Yoga` are rejected.
- Classes under the legacy Yoga Discipline are rejected.
- `YOGA` class type remains legacy read/history data and is rejected for new scheduling.
- Single Sessions and Recurring Schedules based on Yoga are rejected.
- Existing Yoga rows remain available to read and are not deleted or migrated.

## Verification

- Focused suite: 51 tests, 0 failures/errors.
- Full local suite: 526 tests, 0 failures, 50 errors caused only by unavailable Docker/Testcontainers.
- Push-CI and PR-CI: both complete and green, each with unit tests and four integration shards.
- Main CI: run `39180902366`, green with unit tests and four integration shards.
- Post-merge smoke: 51 tests, 0 failures/errors.
- `git diff --check`: passed.

## Notes

Mutation-focused integration fixtures that previously used Yoga were changed to Pilates/GROUP. Historical Yoga read/training fixtures were left unchanged to protect legacy compatibility.
