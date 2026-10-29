alter table accounts
    add column profile_image_url text;

alter table accounts
    add constraint chk_accounts_staff_profile_image
        check (role <> 'MEMBER' or profile_image_url is null);
