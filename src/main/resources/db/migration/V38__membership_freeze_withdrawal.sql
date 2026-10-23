alter table membership_freeze_requests drop constraint chk_membership_freeze_request_status;
alter table membership_freeze_requests add constraint chk_membership_freeze_request_status
    check (status in ('PENDING','APPROVED_PENDING_PAYMENT','SCHEDULED','ACTIVE','COMPLETED','OBSOLETE','REJECTED','WITHDRAWN'));
