create extension if not exists btree_gist;

create table accounts (
    id uuid primary key,
    role varchar(20) not null,
    status varchar(20) not null,
    full_name varchar(200) not null,
    phone varchar(32) not null,
    email varchar(320) not null,
    birth_date date not null,
    password_hash varchar(255) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_accounts_role
        check (role in ('MEMBER', 'COACH', 'RECEPTIONIST', 'MANAGER')),
    constraint chk_accounts_status
        check (status in ('ACTIVE', 'SUSPENDED', 'INACTIVE')),
    constraint chk_accounts_role_lifecycle
        check (
            (role = 'MEMBER' and status in ('ACTIVE', 'SUSPENDED'))
            or (role in ('COACH', 'RECEPTIONIST', 'MANAGER') and status in ('ACTIVE', 'INACTIVE'))
        ),
    constraint chk_accounts_full_name_not_blank check (btrim(full_name) <> ''),
    constraint chk_accounts_phone_not_blank check (btrim(phone) <> ''),
    constraint chk_accounts_email_not_blank check (btrim(email) <> '')
);

create unique index uq_accounts_current_phone
    on accounts (phone)
    where status <> 'INACTIVE';

create unique index uq_accounts_current_email
    on accounts (lower(email))
    where status <> 'INACTIVE';

create sequence member_code_sequence as bigint start with 100001;

create table member_profiles (
    account_id uuid primary key references accounts(id),
    member_code varchar(20) not null unique default ('MB-' || nextval('member_code_sequence')::text),
    profile_image_url text,
    fitness_goal text,
    emergency_contact_name varchar(200),
    emergency_contact_phone varchar(32),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_member_profiles_emergency_contact
        check (
            (emergency_contact_name is null and emergency_contact_phone is null)
            or (emergency_contact_name is not null and emergency_contact_phone is not null)
        ),
    constraint chk_member_profiles_code check (member_code ~ '^MB-[0-9]+$')
);

create function prevent_account_role_change()
returns trigger
language plpgsql
as $$
begin
    if new.role <> old.role then
        raise exception 'Account role is immutable';
    end if;
    return new;
end;
$$;

create trigger trg_accounts_role_immutable
before update of role on accounts
for each row execute function prevent_account_role_change();

create function prevent_account_delete()
returns trigger
language plpgsql
as $$
begin
    raise exception 'Accounts are retained as history; use a status transition instead';
end;
$$;

create trigger trg_accounts_no_hard_delete
before delete on accounts
for each row execute function prevent_account_delete();
