package com.scms.backend.emailverification;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class DatabaseRateLimiter {
	private final JdbcTemplate db; private final EmailVerificationCrypto crypto; private final Clock clock;
	DatabaseRateLimiter(JdbcTemplate db, EmailVerificationCrypto crypto, Clock clock) { this.db=db; this.crypto=crypto; this.clock=clock; }

	void check(String type, String rawKey, int maximum, Duration window) {
		Instant now=clock.instant(); long seconds=window.toSeconds();
		Instant started=Instant.ofEpochSecond((now.getEpochSecond()/seconds)*seconds);
		Integer count=db.queryForObject("""
			insert into email_verification_rate_limits(bucket_type,rate_key_hash,window_started_at,request_count,expires_at,updated_at)
			values (?,?,?,1,?,?) on conflict(bucket_type,rate_key_hash,window_started_at) do update
			set request_count=email_verification_rate_limits.request_count+1,updated_at=excluded.updated_at returning request_count
			""", Integer.class, type, crypto.rateKey(rawKey), Timestamp.from(started), Timestamp.from(started.plus(window)), Timestamp.from(now));
		if (count != null && count > maximum) throw new EmailVerificationException(HttpStatus.TOO_MANY_REQUESTS,
			"RATE_LIMITED", "Too many requests.", Math.max(1, started.plus(window).getEpochSecond()-now.getEpochSecond()));
	}
}
