alter table bookings drop constraint chk_bookings_cancellation_source;

alter table bookings add constraint chk_bookings_cancellation_source check (
    cancellation_source is null
    or cancellation_source in (
        'MEMBER', 'RECEPTIONIST', 'MEMBER_SUSPENDED', 'SESSION_CANCELLED',
        'MEMBERSHIP_TERMINATED', 'MEMBERSHIP_FREEZE', 'COACH_ASSIGNMENT_CHANGED'
    )
);
