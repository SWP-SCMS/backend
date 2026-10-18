alter table membership_plans
    drop constraint if exists chk_membership_plans_code;

alter table membership_plans
    alter column plan_code type varchar(50),
    add column if not exists status varchar(20) not null default 'ACTIVE',
    add column if not exists created_by_account_id uuid references accounts(id),
    add column if not exists updated_at timestamptz not null default current_timestamp,
    add column if not exists version bigint not null default 0;

alter table membership_offers alter column plan_code type varchar(50);
alter table membership_orders alter column plan_code_snapshot type varchar(50);
alter table memberships alter column plan_code_snapshot type varchar(50);

alter table membership_plans
    add constraint chk_membership_plans_status check (status in ('ACTIVE', 'INACTIVE'));

create table membership_benefits (
    code varchar(50) primary key,
    display_name varchar(150) not null,
    description text not null,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamptz not null default current_timestamp,
    constraint chk_membership_benefits_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_membership_benefits_display_name_not_blank check (btrim(display_name) <> '')
);

create table membership_plan_benefits (
    plan_code varchar(50) not null references membership_plans(plan_code),
    benefit_code varchar(50) not null references membership_benefits(code),
    primary key (plan_code, benefit_code)
);

create table discipline_required_benefits (
    discipline_id uuid not null references disciplines(id),
    benefit_code varchar(50) not null references membership_benefits(code),
    primary key (discipline_id, benefit_code)
);

insert into membership_benefits (code, display_name, description)
values
    ('CENTER_ACCESS', 'Ra/vào trung tâm', 'Cho phép check-in tại trung tâm'),
    ('CLASS_BOOKING', 'Đặt lịch lớp học', 'Cho phép đặt các buổi học phù hợp'),
    ('POOL_ACCESS', 'Sử dụng hồ bơi', 'Cho phép đặt Session thuộc Discipline yêu cầu hồ bơi'),
    ('MASSAGE_ACCESS', 'Sử dụng dịch vụ massage', 'Cho phép đặt Session Massage'),
    ('OUTDOOR_SPORT_ACCESS', 'Thể thao ngoài trời', 'Cho phép đặt Session thể thao ngoài trời'),
    ('PERSONAL_COACHING', 'Huấn luyện viên cá nhân', 'Cho phép sử dụng dịch vụ huấn luyện cá nhân'),
    ('TRAINING_PLAN', 'Kế hoạch luyện tập', 'Cho phép phát sinh Training Plan mới'),
    ('TRAINING_FEEDBACK', 'Nhận xét luyện tập', 'Cho phép phát sinh Feedback mới'),
    ('TRAINING_RESULT', 'Kết quả luyện tập', 'Cho phép phát sinh Training Result mới')
on conflict (code) do nothing;

insert into membership_plan_benefits (plan_code, benefit_code)
select p.plan_code, b.code
from membership_plans p
join membership_benefits b on b.code = 'CENTER_ACCESS'
where p.plan_code in ('BASIC', 'PLUS')
on conflict do nothing;

insert into membership_plan_benefits (plan_code, benefit_code)
select 'PLUS', code
from membership_benefits
where code in ('CLASS_BOOKING', 'PERSONAL_COACHING', 'TRAINING_PLAN', 'TRAINING_FEEDBACK', 'TRAINING_RESULT')
on conflict do nothing;
