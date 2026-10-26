alter table email_outbox
    add column payload_format varchar(32) not null default 'OTP_V1',
    add column delivery_provider varchar(16) not null default 'smtp',
    add column provider_message_id varchar(128);

alter table email_outbox add constraint chk_email_outbox_payload_format
    check (payload_format in ('OTP_V1', 'EMAIL_REQUEST_V1'));

alter table email_outbox add constraint chk_email_outbox_delivery_provider
    check (delivery_provider in ('smtp', 'resend'));

create index ix_email_outbox_provider_active
    on email_outbox(delivery_provider, status)
    where status in ('PENDING', 'FAILED', 'PROCESSING');
