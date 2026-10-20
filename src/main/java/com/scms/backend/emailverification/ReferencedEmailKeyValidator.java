package com.scms.backend.emailverification;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ReferencedEmailKeyValidator implements ApplicationRunner {
	private final JdbcTemplate db;private final EmailVerificationProperties properties;
	ReferencedEmailKeyValidator(JdbcTemplate db,EmailVerificationProperties properties){this.db=db;this.properties=properties;}
	@Override public void run(ApplicationArguments args){if(!properties.isEnabled())return;
		List<String> hmac=db.queryForList("select distinct hmac_key_version from email_verification_challenges where consumed_at is null and superseded_at is null",String.class);
		List<String> encryption=db.queryForList("select distinct encryption_key_version from email_outbox where status in ('PENDING','FAILED','PROCESSING')",String.class);
		if(!properties.hmacKeyMap().keySet().containsAll(hmac))throw new IllegalStateException("A referenced email OTP HMAC key version is missing");
		if(!properties.encryptionKeyMap().keySet().containsAll(encryption))throw new IllegalStateException("A referenced email OTP encryption key version is missing");}
}
