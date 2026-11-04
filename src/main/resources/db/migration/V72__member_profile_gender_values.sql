-- The optional gender field now supports only MALE/FEMALE.
-- Legacy OTHER values become unspecified rather than being assigned a gender.
UPDATE member_profiles SET gender = NULL WHERE gender = 'OTHER';
ALTER TABLE member_profiles DROP CONSTRAINT ck_member_profile_gender;
ALTER TABLE member_profiles ADD CONSTRAINT ck_member_profile_gender
    CHECK (gender IN ('MALE', 'FEMALE'));
