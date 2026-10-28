package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailDeliveryPropertiesTests {

	@Test
	void rejectsNonSmtpDeliveryProvider() {
		EmailDeliveryProperties properties = new EmailDeliveryProperties();
		properties.setProvider("resend");

		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("SCMS_EMAIL_PROVIDER must be smtp");
	}

	@Test
	void requiresSenderToMatchSmtpUsernameUnlessCustomFromIsAllowed() {
		EmailDeliveryProperties properties = new EmailDeliveryProperties();
		properties.setFrom("alerts@example.test");
		properties.setSmtpUsername("mailer@example.test");

		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("SMTP_FROM must equal SMTP_USERNAME unless SMTP_ALLOW_CUSTOM_FROM=true");

		properties.setAllowCustomFrom(true);
		properties.validate();
		assertThat(properties.getFrom()).isEqualTo("alerts@example.test");
	}

	@Test
	void requiresStagingOverrideAndNeverAppliesItInProduction() {
		EmailDeliveryProperties properties = new EmailDeliveryProperties();
		properties.setStaging(true);
		properties.setFrom("mailer@example.test");
		properties.setSmtpUsername("mailer@example.test");
		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("SMTP_RECIPIENT_OVERRIDE is required in staging");

		properties.setRecipientOverride("otp-test@example.test");
		properties.validate();
		assertThat(properties.recipient("real@example.test")).isEqualTo("otp-test@example.test");

		properties.setProduction(true);
		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("SMTP_RECIPIENT_OVERRIDE cannot be enabled in production");
	}

	@Test
	void requiresConfiguredSenderInStagingAndProduction() {
		EmailDeliveryProperties properties = new EmailDeliveryProperties();
		properties.setStaging(true);
		properties.setRecipientOverride("otp-test@example.test");
		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("SMTP_FROM and SMTP_USERNAME are required in staging and production");
	}
}
