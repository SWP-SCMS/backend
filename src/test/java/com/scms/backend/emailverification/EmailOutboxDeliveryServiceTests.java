package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class EmailOutboxDeliveryServiceTests {
	@Test
	void schedulesInitialAttemptPlusThreeRetries() {
		assertThat(EmailOutboxDeliveryService.retryDelay(1, null)).isEqualTo(Duration.ofSeconds(30));
		assertThat(EmailOutboxDeliveryService.retryDelay(2, null)).isEqualTo(Duration.ofSeconds(120));
		assertThat(EmailOutboxDeliveryService.retryDelay(3, null)).isEqualTo(Duration.ofSeconds(300));
		assertThat(EmailOutboxDeliveryService.retryDelay(4, null)).isNull();
	}

	@Test
	void respectsLongerProviderRetryAfter() {
		assertThat(EmailOutboxDeliveryService.retryDelay(1, Duration.ofSeconds(75)))
			.isEqualTo(Duration.ofSeconds(75));
	}

	@Test
	void rejectsInvalidAttemptCounter() {
		assertThatThrownBy(() -> EmailOutboxDeliveryService.retryDelay(0, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void reportsOnlyThresholdsThatAreExceeded() {
		var metrics = new EmailOutboxDeliveryService.OutboxMetrics(1, 1, 1, 1, 1, 40);
		assertThat(EmailOutboxDeliveryService.alertLevels(metrics)).containsExactly("WARN", "ERROR");
	}

	@Test
	void doesNotRaiseAlertsAtWarnBoundariesOrBelowErrorSentThreshold() {
		var metrics = new EmailOutboxDeliveryService.OutboxMetrics(0, 0, 0, 0, 0, 39);
		assertThat(EmailOutboxDeliveryService.alertLevels(metrics)).isEmpty();
	}

	@Test
	void warnsForFailedPendingAndProcessingButErrorsOnlyAtTerminalActiveOrFortySent() {
		assertThat(EmailOutboxDeliveryService.alertLevels(
			new EmailOutboxDeliveryService.OutboxMetrics(0, 0, 0, 0, 0, 40))).containsExactly("ERROR");
		assertThat(EmailOutboxDeliveryService.alertLevels(
			new EmailOutboxDeliveryService.OutboxMetrics(0, 0, 0, 1, 0, 0))).containsExactly("ERROR");
		assertThat(EmailOutboxDeliveryService.alertLevels(
			new EmailOutboxDeliveryService.OutboxMetrics(0, 0, 0, 0, 1, 0))).containsExactly("ERROR");
		assertThat(EmailOutboxDeliveryService.alertLevels(
			new EmailOutboxDeliveryService.OutboxMetrics(0, 1, 0, 0, 0, 0))).containsExactly("WARN");
	}
}
