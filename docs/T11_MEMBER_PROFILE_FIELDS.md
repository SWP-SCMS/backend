# T11 — Member profile backend contract

GET/PATCH `/api/v1/members/me/profile` and receptionist member profile GET/PATCH support:

| Field | Meaning | Allowed value / limit |
| --- | --- | --- |
| `gender` | Giới tính | `MALE`, `FEMALE`, or null |
| `address` | Địa chỉ cư trú | 500 characters |
| `physicalConditionNotes` | Ghi chú thể chất và tiền sử chấn thương | 2000 characters |
| `emergencyContactRelationship` | Quan hệ với người liên hệ khẩn cấp | 100 characters |
| `emergencyContactTimeNotes` | Ghi chú khung giờ liên hệ | 500 characters |

All five fields are optional. PATCH omission preserves the value; explicit null clears it.
Strings are trimmed. Blank strings, invalid gender and over-limit values return HTTP 400
`VALIDATION_ERROR` with field-specific errors. FE should convert an emptied input to null.
Relationship/time notes are independent optional text; the existing emergency name/phone pair rule is unchanged.

Responses also include read-only `createdAt` from the account creation timestamp.
The existing account, membership and authentication rules are unchanged.

Migration: `V71__member_profile_additional_details.sql` adds nullable columns without changing existing data.
Local BE restarted on 2026-11-05; Flyway successfully applied V71. FE form wiring is complete.

Validation (2026-11-05): build and 21 targeted tests passed (MemberProfileServiceTests,
ProfileImageUploadServiceTests, ProfileImageUploadControllerTests). Mockito was loaded with
`-javaagent` and tests ran outside the Windows sandbox because its temporary-file atomic moves
returned AccessDeniedException. Log: `target/t11-tests-verified.log`.

The added PostgreSQL integration test covers PATCH → persistence-context clear → GET, Unicode text, omission and null clearing;
it requires Docker/Testcontainers to execute and has not been run on this machine (Docker unavailable).
Chrome MCP runtime verification against the local PostgreSQL database passed: save all five fields,
reload, partial updates, clear with null, and preserve existing profile fields. See frontend
`docs/T11_PROFILE_QA_2026-11-05.md` for evidence.

Follow-up: V72 restricts gender to MALE/FEMALE/null and maps legacy OTHER to unspecified.
V72 applied locally; 10 MemberProfileServiceTests passed. Chrome verified OTHER returns HTTP 400.
