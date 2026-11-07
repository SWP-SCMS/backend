ALTER TABLE accounts
    ADD COLUMN staff_emergency_contact_name varchar(200),
    ADD COLUMN staff_emergency_contact_phone varchar(32),
    ADD COLUMN staff_emergency_contact_relationship varchar(100);
ALTER TABLE accounts ADD CONSTRAINT ck_staff_emergency_contact_pair
    CHECK ((staff_emergency_contact_name IS NULL) = (staff_emergency_contact_phone IS NULL));
