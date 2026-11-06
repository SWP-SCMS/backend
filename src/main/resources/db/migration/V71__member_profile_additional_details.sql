ALTER TABLE member_profiles
    ADD COLUMN gender varchar(16),
    ADD COLUMN address varchar(500),
    ADD COLUMN physical_condition_notes varchar(2000),
    ADD COLUMN emergency_contact_relationship varchar(100),
    ADD COLUMN emergency_contact_time_notes varchar(500);

ALTER TABLE member_profiles
    ADD CONSTRAINT ck_member_profile_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));
