package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class EmailVerificationCryptoTests {
	@Test
	void oldKeyVersionsRemainUsableAfterActiveVersionRotation(){
		EmailVerificationProperties properties=validProperties();
		properties.setHmacKeys("v1:"+key('a')+",v2:"+key('b'));
		properties.setEncryptionKeys("v1:"+key('c')+",v2:"+key('d'));
		properties.setHmacActiveVersion("v2");properties.setEncryptionActiveVersion("v2");properties.validate();
		EmailVerificationCrypto crypto=new EmailVerificationCrypto(properties);
		String digest=crypto.otpDigest("123456","v1");
		var encrypted=crypto.encryptOtp("123456","v1");
		assertThat(crypto.matchesOtp("123456",digest,"v1")).isTrue();
		assertThat(crypto.decryptOtp(encrypted.ciphertext(),encrypted.nonce(),"v1")).isEqualTo("123456");
	}

	@Test
	void enabledFeatureFailsFastForMissingOrInvalidKeys(){
		EmailVerificationProperties missing=validProperties();missing.setHmacActiveVersion("missing");
		assertThatThrownBy(missing::validate).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("HMAC key version");
		EmailVerificationProperties shortRateKey=validProperties();shortRateKey.setRateLimitKey(Base64.getEncoder().encodeToString(new byte[8]));
		assertThatThrownBy(shortRateKey::validate).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("rate-limit key must be 32 bytes");
	}

	private EmailVerificationProperties validProperties(){
		EmailVerificationProperties properties=new EmailVerificationProperties();properties.setEnabled(true);
		properties.setHmacActiveVersion("v1");properties.setEncryptionActiveVersion("v1");
		properties.setHmacKeys("v1:"+key('a'));properties.setEncryptionKeys("v1:"+key('b'));
		properties.setRateLimitKey(key('r'));return properties;
	}
	private String key(char value){return Base64.getEncoder().encodeToString(String.valueOf(value).repeat(32).getBytes(StandardCharsets.US_ASCII));}
}
