create table disciplines (
    id uuid primary key,
    name varchar(150) not null,
    description text,
    status varchar(20) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint uq_disciplines_name unique (name),
    constraint chk_disciplines_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_disciplines_name_not_blank check (btrim(name) <> '')
);

create table sport_classes (
    id uuid primary key,
    discipline_id uuid not null references disciplines(id),
    name varchar(150) not null,
    class_type varchar(20) not null,
    description text,
    status varchar(20) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint uq_sport_classes_discipline_name unique (discipline_id, name),
    constraint chk_sport_classes_type check (class_type in ('GROUP', 'YOGA', 'PT_1_1')),
    constraint chk_sport_classes_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_sport_classes_name_not_blank check (btrim(name) <> '')
);

create table rooms (
    id uuid primary key,
    name varchar(150) not null,
    capacity integer not null,
    status varchar(20) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint uq_rooms_name unique (name),
    constraint chk_rooms_capacity check (capacity > 0),
    constraint chk_rooms_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_rooms_name_not_blank check (btrim(name) <> '')
);

create table recurring_schedules (
    id uuid primary key,
    sport_class_id uuid not null references sport_classes(id),
    teaching_coach_account_id uuid not null references accounts(id),
    room_id uuid not null references rooms(id),
    start_date date not null,
    weekdays smallint[] not null,
    start_time time not null,
    end_time time not null,
    session_capacity integer not null,
    session_count smallint not null default 30,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,

    constraint chk_recurring_schedules_weekdays_not_empty check (cardinality(weekdays) > 0),
    constraint chk_recurring_schedules_weekdays_valid check (weekdays <@ array[1, 2, 3, 4, 5, 6, 7]::smallint[]),
    constraint chk_recurring_schedules_time check (end_time > start_time),
    constraint chk_recurring_schedules_capacity check (session_capacity > 0),
    constraint chk_recurring_schedules_session_count check (session_count = 30)
);

create table class_sessions (
    id uuid primary key,
    sport_class_id uuid not null references sport_classes(id),
    recurring_schedule_id uuid references recurring_schedules(id),
    teaching_coach_account_id uuid not null references accounts(id),
    room_id uuid not null references rooms(id),
    start_time timestamptz not null,
    end_time timestamptz not null,
    capacity integer not null,
    status varchar(20) not null,
    cancelled_at timestamptz,
    cancellation_reason text,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_class_sessions_time check (end_time > start_time),
    constraint chk_class_sessions_capacity check (capacity > 0),
    constraint chk_class_sessions_status check (status in ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    constraint chk_class_sessions_cancellation check (
        (status = 'CANCELLED' and cancelled_at is not null)
        or (status <> 'CANCELLED' and cancelled_at is null)
    ),
    constraint ex_class_sessions_room_time exclude using gist (
        room_id with =,
        tstzrange(start_time, end_time, '[)') with &&
    ) where (status in ('SCHEDULED', 'IN_PROGRESS')),
    constraint ex_class_sessions_coach_time exclude using gist (
        teaching_coach_account_id with =,
        tstzrange(start_time, end_time, '[)') with &&
    ) where (status in ('SCHEDULED', 'IN_PROGRESS'))
);

create index idx_class_sessions_class_start_time on class_sessions (sport_class_id, start_time);

create index idx_class_sessions_coach_start_time
    on class_sessions (teaching_coach_account_id, start_time)
    where status in ('SCHEDULED', 'IN_PROGRESS');

create table bookings (
    id uuid primary key,
    class_session_id uuid not null references class_sessions(id),
    member_account_id uuid not null references accounts(id),
    membership_id uuid not null,
    status varchar(20) not null,
    booked_by_account_id uuid not null references accounts(id),
    cancelled_by_account_id uuid references accounts(id),
    cancellation_source varchar(30),
    cancelled_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_bookings_status check (status in ('BOOKED', 'CANCELLED')),
    constraint chk_bookings_cancellation check (
        (status = 'BOOKED' and cancelled_at is null and cancellation_source is null)
        or (status = 'CANCELLED' and cancelled_at is not null and cancellation_source is not null)
    ),
    constraint chk_bookings_cancellation_source check (
        cancellation_source is null
        or cancellation_source in ('MEMBER', 'RECEPTIONIST', 'MEMBER_SUSPENDED', 'SESSION_CANCELLED')
    ),
    constraint fk_bookings_membership_owner
        foreign key (membership_id, member_account_id) references memberships (id, member_account_id)
);

create unique index uq_bookings_member_session_booked
    on bookings (member_account_id, class_session_id)
    where status = 'BOOKED';

create index idx_bookings_session_status on bookings (class_session_id, status);

create index idx_bookings_member_created_at on bookings (member_account_id, created_at desc);

create table center_visits (
    id uuid primary key,
    member_account_id uuid not null references accounts(id),
    membership_id uuid not null,
    checked_in_by_account_id uuid not null references accounts(id),
    checked_in_at timestamptz not null default current_timestamp,
    created_at timestamptz not null default current_timestamp,

    constraint fk_center_visits_membership_owner
        foreign key (membership_id, member_account_id) references memberships (id, member_account_id)
);

create index idx_center_visits_member_checked_in_at
    on center_visits (member_account_id, checked_in_at desc);

create table attendance (
    id uuid primary key,
    booking_id uuid not null unique references bookings(id),
    status varchar(20) not null,
    center_visit_id uuid references center_visits(id),
    recorded_by_account_id uuid references accounts(id),
    recording_source varchar(20) not null default 'SYSTEM',
    recorded_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_attendance_status check (status in ('PRESENT', 'ABSENT')),
    constraint chk_attendance_present_visit check (
        status <> 'PRESENT' or center_visit_id is not null
    ),
    constraint chk_attendance_recording_source check (
        (recording_source = 'SYSTEM' and recorded_by_account_id is null)
        or (recording_source = 'COACH' and recorded_by_account_id is not null)
    )
);

create function validate_attendance_visit_owner()
returns trigger
language plpgsql
as $$
declare
    booking_member_id uuid;
    visit_member_id uuid;
    visit_checked_in_at timestamptz;
    session_start_time timestamptz;
begin
    if new.status = 'PRESENT' and new.center_visit_id is not null then
        select bookings.member_account_id, class_sessions.start_time
        into booking_member_id, session_start_time
        from bookings
        join class_sessions on class_sessions.id = bookings.class_session_id
        where bookings.id = new.booking_id;

        select member_account_id, checked_in_at
        into visit_member_id, visit_checked_in_at
        from center_visits
        where id = new.center_visit_id;

        if booking_member_id is distinct from visit_member_id then
            raise exception 'Attendance Center Visit must belong to the booked Member';
        end if;

        if visit_checked_in_at > current_timestamp then
            raise exception 'Attendance Center Visit cannot be in the future';
        end if;

        if (visit_checked_in_at at time zone 'Asia/Ho_Chi_Minh')::date
            <> (session_start_time at time zone 'Asia/Ho_Chi_Minh')::date then
            raise exception 'Attendance Center Visit must be on the Session operating date';
        end if;
    end if;
    return new;
end;
$$;

create trigger trg_attendance_visit_owner
before insert or update of booking_id, status, center_visit_id on attendance
for each row execute function validate_attendance_visit_owner();
