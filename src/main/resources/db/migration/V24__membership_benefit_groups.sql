create table membership_benefit_groups (
    code varchar(50) primary key,
    display_name varchar(150) not null,
    description text,
    display_order integer not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint chk_membership_benefit_groups_display_name check (btrim(display_name) <> ''),
    constraint chk_membership_benefit_groups_display_order check (display_order >= 0)
);

insert into membership_benefit_groups (code, display_name, description, display_order)
values
    ('PERSONAL_TRAINING', 'Huấn luyện cá nhân', null, 60),
    ('TRAINING_TRACKING', 'Theo dõi luyện tập', null, 70);

alter table membership_benefits
    add column group_code varchar(50),
    add column display_order integer;

update membership_benefits
set group_code = case
        when code in ('PERSONAL_COACHING', 'TRAINING_PLAN') then 'PERSONAL_TRAINING'
        when code in ('TRAINING_RESULT', 'TRAINING_FEEDBACK') then 'TRAINING_TRACKING'
        else null
    end,
    display_order = case code
        when 'CENTER_ACCESS' then 10
        when 'CLASS_BOOKING' then 20
        when 'POOL_ACCESS' then 30
        when 'MASSAGE_ACCESS' then 40
        when 'OUTDOOR_SPORT_ACCESS' then 50
        when 'PERSONAL_COACHING' then 10
        when 'TRAINING_RESULT' then 10
        when 'TRAINING_PLAN' then 20
        when 'TRAINING_FEEDBACK' then 20
        else 1000
    end;

alter table membership_benefits
    alter column display_order set not null,
    add constraint fk_membership_benefits_group
        foreign key (group_code) references membership_benefit_groups(code),
    add constraint chk_membership_benefits_display_order check (display_order >= 0);
