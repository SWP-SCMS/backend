-- Additive: existing reconciliation requests and payments are unchanged.
create table payment_evidence_images (
    id uuid primary key,
    payment_id uuid not null references payments(id),
    uploaded_by uuid not null references accounts(id),
    content_type varchar(20) not null check (content_type in ('image/png', 'image/jpeg')),
    sha256 varchar(64) not null,
    image_data bytea not null check (octet_length(image_data) between 1 and 4194304),
    created_at timestamptz not null default current_timestamp,
    unique (payment_id, sha256)
);
