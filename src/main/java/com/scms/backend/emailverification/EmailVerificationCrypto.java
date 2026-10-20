package com.scms.backend.emailverification;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
class EmailVerificationCrypto {
	private final EmailVerificationProperties properties;
	private final SecureRandom random = new SecureRandom();

	EmailVerificationCrypto(EmailVerificationProperties properties) { this.properties = properties; }

	String otp() { return "%06d".formatted(random.nextInt(1_000_000)); }
	String token() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
	String sha256(String value) { return hex(digest("SHA-256", value.getBytes(StandardCharsets.UTF_8))); }
	boolean secureHashMatch(String raw, String expectedHash) { return MessageDigest.isEqual(
		sha256(raw).getBytes(StandardCharsets.US_ASCII), expectedHash.getBytes(StandardCharsets.US_ASCII)); }
	String otpDigest(String otp, String version) { return hex(hmac(properties.hmacKeyMap().get(version), otp)); }
	boolean matchesOtp(String otp, String expected, String version) {
		return MessageDigest.isEqual(otpDigest(otp, version).getBytes(StandardCharsets.US_ASCII),
			expected.getBytes(StandardCharsets.US_ASCII));
	}
	String rateKey(String value) { return hex(hmac(properties.rateLimitKeyBytes(), value)); }

	Encrypted encryptOtp(String otp, String version) {
		try {
			byte[] nonce = new byte[12]; random.nextBytes(nonce);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(properties.encryptionKeyMap().get(version), "AES"),
				new GCMParameterSpec(128, nonce));
			return new Encrypted(cipher.doFinal(otp.getBytes(StandardCharsets.US_ASCII)), nonce);
		} catch (Exception exception) { throw new IllegalStateException("Unable to encrypt email OTP", exception); }
	}

	String decryptOtp(byte[] ciphertext, byte[] nonce, String version) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(properties.encryptionKeyMap().get(version), "AES"),
				new GCMParameterSpec(128, nonce));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.US_ASCII);
		} catch (Exception exception) { throw new IllegalStateException("Unable to decrypt email OTP", exception); }
	}

	private byte[] hmac(byte[] key, String value) {
		try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		} catch (Exception exception) { throw new IllegalStateException("Unable to calculate HMAC", exception); }
	}
	private byte[] digest(String algorithm, byte[] value) { try { return MessageDigest.getInstance(algorithm).digest(value); }
		catch (Exception exception) { throw new IllegalStateException(exception); } }
	private String hex(byte[] value) { return java.util.HexFormat.of().formatHex(value); }
	record Encrypted(byte[] ciphertext, byte[] nonce) { }
}
