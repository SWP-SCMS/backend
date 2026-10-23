alter table center_visits drop constraint chk_center_visits_checkout_source;
alter table center_visits add constraint chk_center_visits_checkout_source check (
    checkout_source is null
    or checkout_source in ('MEMBER', 'RECEPTIONIST', 'AUTO_REENTRY', 'MIGRATION', 'MEMBERSHIP_TERMINATION')
);

alter table bookings drop constraint chk_bookings_cancellation_source;
alter table bookings add constraint chk_bookings_cancellation_source check (
    cancellation_source is null
    or cancellation_source in ('MEMBER', 'RECEPTIONIST', 'MEMBER_SUSPENDED', 'SESSION_CANCELLED', 'MEMBERSHIP_TERMINATED')
);
