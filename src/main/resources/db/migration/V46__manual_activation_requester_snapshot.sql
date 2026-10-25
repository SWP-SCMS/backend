alter table email_verification_manual_activation_requests
    add column requested_by_name_snapshot varchar(200),
    add column requested_by_role_snapshot varchar(20);

update email_verification_manual_activation_requests request
set requested_by_name_snapshot = account.full_name,
    requested_by_role_snapshot = account.role
from accounts account
where account.id = request.requested_by_account_id;

alter table email_verification_manual_activation_requests
    alter column requested_by_name_snapshot set not null,
    alter column requested_by_role_snapshot set not null;
