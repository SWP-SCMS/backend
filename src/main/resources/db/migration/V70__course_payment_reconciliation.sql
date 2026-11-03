-- Append-only decisions and exact retry results; historical payments are unchanged.
create table course_payment_reconciliations (
 id uuid primary key,
 actor_account_id uuid not null references accounts(id),
 course_payment_id uuid not null references course_payments(id),
 idempotency_key varchar(128) not null check (btrim(idempotency_key) <> ''),
 request_json jsonb not null,
 response_json jsonb not null,
 changed_payment boolean not null,
 created_at timestamptz not null,
 unique(actor_account_id,course_payment_id,idempotency_key),
 check (jsonb_typeof(request_json)='object' and request_json ?& array['status','evidence','reason']),
 check (jsonb_typeof(response_json)='object'),
 check (request_json->>'status' in ('PAID','FAILED')),
 check (length(btrim(request_json->>'evidence')) > 0),
 check (length(btrim(request_json->>'reason')) > 0)
);
create index idx_course_payment_reconciliations_payment on course_payment_reconciliations(course_payment_id,created_at);
