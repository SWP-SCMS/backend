package com.scms.backend.emailverification;

import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scms.email-verification")
public class EmailVerificationProperties {
	private boolean enabled;
	private Duration registrationTtl = Duration.ofHours(24);
	private Duration otpTtl = Duration.ofMinutes(10);
	private Duration resendCooldown = Duration.ofSeconds(60);
	private int maxAttempts = 5;
	private String hmacActiveVersion = "";
	private String encryptionActiveVersion = "";
	private String hmacKeys = "";
	private String encryptionKeys = "";
	private String rateLimitKey = "";
	private boolean cookieSecure = true;
	private String cookieSameSite = "Lax";

	@PostConstruct
	void validate() {
		if (!enabled) return;
		if (registrationTtl.isNegative() || registrationTtl.isZero() || otpTtl.isNegative() || otpTtl.isZero()
				|| resendCooldown.isNegative() || maxAttempts < 1) {
			throw new IllegalStateException("Email verification durations and attempts must be positive");
		}
		if (!keyMap(hmacKeys, 32).containsKey(hmacActiveVersion))
			throw new IllegalStateException("Active email OTP HMAC key version is missing");
		if (!keyMap(encryptionKeys, 32).containsKey(encryptionActiveVersion))
			throw new IllegalStateException("Active email OTP encryption key version is missing");
		decodeKey(rateLimitKey, 32, "rate-limit");
	}

	Map<String, byte[]> hmacKeyMap() { return keyMap(hmacKeys, 32); }
	Map<String, byte[]> encryptionKeyMap() { return keyMap(encryptionKeys, 32); }
	byte[] rateLimitKeyBytes() { return decodeKey(rateLimitKey, 32, "rate-limit"); }

	private Map<String, byte[]> keyMap(String value, int bytes) {
		Map<String, byte[]> result = new LinkedHashMap<>();
		if (value == null || value.isBlank()) return result;
		for (String entry : value.split(",")) {
			int separator = entry.indexOf(':');
			if (separator < 1) throw new IllegalStateException("Key map entries must use version:base64");
			result.put(entry.substring(0, separator).trim(),
				decodeKey(entry.substring(separator + 1).trim(), bytes, entry.substring(0, separator)));
		}
		return Map.copyOf(result);
	}

	private byte[] decodeKey(String encoded, int bytes, String label) {
		try {
			byte[] decoded = Base64.getDecoder().decode(encoded == null ? "" : encoded);
			if (decoded.length != bytes) throw new IllegalStateException(label + " key must be " + bytes + " bytes");
			return decoded;
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException(label + " key must be valid Base64", exception);
		}
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public Duration getRegistrationTtl() { return registrationTtl; }
	public void setRegistrationTtl(Duration value) { registrationTtl = value; }
	public Duration getOtpTtl() { return otpTtl; }
	public void setOtpTtl(Duration value) { otpTtl = value; }
	public Duration getResendCooldown() { return resendCooldown; }
	public void setResendCooldown(Duration value) { resendCooldown = value; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int value) { maxAttempts = value; }
	public String getHmacActiveVersion() { return hmacActiveVersion; }
	public void setHmacActiveVersion(String value) { hmacActiveVersion = value; }
	public String getEncryptionActiveVersion() { return encryptionActiveVersion; }
	public void setEncryptionActiveVersion(String value) { encryptionActiveVersion = value; }
	public void setHmacKeys(String value) { hmacKeys = value; }
	public void setEncryptionKeys(String value) { encryptionKeys = value; }
	public void setRateLimitKey(String value) { rateLimitKey = value; }
	public boolean isCookieSecure() { return cookieSecure; }
	public void setCookieSecure(boolean value) { cookieSecure = value; }
	public String getCookieSameSite() { return cookieSameSite; }
	public void setCookieSameSite(String value) { cookieSameSite = value; }
}
