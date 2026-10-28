package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
