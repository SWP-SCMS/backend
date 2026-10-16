package com.scms.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationOutboxDispatcherTests {

	@Test
	void deliveryFailureUsesInjectedServerClockForUpdatedAt() {
		Instant now = Instant.parse("2026-10-12T04:00:00Z");
		NotificationOutboxRepository outbox = mock(NotificationOutboxRepository.class);
		NotificationProvider provider = mock(NotificationProvider.class);
		NotificationOutboxDeliveryService delivery = new NotificationOutboxDeliveryService(outbox, provider,
			Clock.fixed(now, ZoneOffset.UTC));
		UUID eventId = UUID.randomUUID();
		Instant nextAttemptAt = now.plusSeconds(30);

		delivery.markFailed(eventId, nextAttemptAt, "inbox unavailable");

		verify(outbox).markFailed(eventId, nextAttemptAt, now, "inbox unavailable");
	}

	@Test
	void terminalFailureUsesInjectedServerClockAndClearsRetrySchedule() {
		Instant now = Instant.parse("2026-10-12T04:00:00Z");
		NotificationOutboxRepository outbox = mock(NotificationOutboxRepository.class);
		NotificationOutboxDeliveryService delivery = new NotificationOutboxDeliveryService(outbox,
			mock(NotificationProvider.class), Clock.fixed(now, ZoneOffset.UTC));
		UUID eventId = UUID.randomUUID();

		delivery.markTerminalFailed(eventId, "inbox unavailable");

		verify(outbox).markTerminalFailed(eventId, now, "inbox unavailable");
	}

	@Test
	void failedProviderDeliveryIsRetriedWithBackoffWithoutEscapingToBusinessCaller() {
		Instant now = Instant.parse("2026-10-12T04:00:00Z");
		NotificationOutboxDeliveryService delivery = mock(NotificationOutboxDeliveryService.class);
		NotificationOutboxEvent event = new NotificationOutboxEvent(UUID.randomUUID(), "PAYMENT_PAID:payment-1",
			UUID.randomUUID(), "PAYMENT_PAID", "PAYMENT", UUID.randomUUID(), "{}", 1);
		when(delivery.claimNext(now, now.plusSeconds(60))).thenReturn(Optional.of(event), Optional.empty());
		doThrow(new IllegalStateException("inbox unavailable")).when(delivery).deliver(event, now);
		NotificationOutboxDispatcher dispatcher = new NotificationOutboxDispatcher(delivery,
			Clock.fixed(now, ZoneOffset.UTC));

		dispatcher.dispatchAvailable();

		ArgumentCaptor<Instant> nextAttempt = ArgumentCaptor.forClass(Instant.class);
		verify(delivery).markFailed(eq(event.id()), nextAttempt.capture(), eq("inbox unavailable"));
		assertThat(nextAttempt.getValue()).isEqualTo(now.plusSeconds(30));
	}

	@Test
	void unavailableOutboxDoesNotEscapeTheDispatcher() {
		Instant now = Instant.parse("2026-10-12T04:00:00Z");
		NotificationOutboxDeliveryService delivery = mock(NotificationOutboxDeliveryService.class);
		when(delivery.claimNext(now, now.plusSeconds(60))).thenThrow(new IllegalStateException("database unavailable"));
		NotificationOutboxDispatcher dispatcher = new NotificationOutboxDispatcher(delivery,
			Clock.fixed(now, ZoneOffset.UTC));

		assertThatCode(dispatcher::dispatchAvailable).doesNotThrowAnyException();
	}

	@Test
	void tenthFailedAttemptBecomesTerminalAndIsNotScheduledAgain() {
		Instant now = Instant.parse("2026-10-12T04:00:00Z");
		NotificationOutboxDeliveryService delivery = mock(NotificationOutboxDeliveryService.class);
		NotificationOutboxEvent event = new NotificationOutboxEvent(UUID.randomUUID(), "PAYMENT_PAID:payment-1",
			UUID.randomUUID(), "PAYMENT_PAID", "PAYMENT", UUID.randomUUID(), "{}", 10);
		when(delivery.claimNext(now, now.plusSeconds(60))).thenReturn(Optional.of(event), Optional.empty());
		doThrow(new IllegalStateException("inbox unavailable")).when(delivery).deliver(event, now);
		NotificationOutboxDispatcher dispatcher = new NotificationOutboxDispatcher(delivery,
			Clock.fixed(now, ZoneOffset.UTC));

		dispatcher.dispatchAvailable();

		verify(delivery).markTerminalFailed(event.id(), "inbox unavailable");
		verify(delivery, never()).markFailed(eq(event.id()), org.mockito.ArgumentMatchers.any(),
			eq("inbox unavailable"));
	}
}
