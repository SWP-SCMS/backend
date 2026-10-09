package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentFulfillmentServiceTests {

	@Test
	void paymentForUpdateLocksOrderThenPaymentBeforeReadingProjection() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Map<String, Object> projection = Map.of(
			"payment_id", paymentId,
			"order_id", orderId,
			"payment_status", "PENDING",
			"order_status", "PENDING_PAYMENT");
		when(db.queryForObject("select order_id from payments where id=?", UUID.class, paymentId))
			.thenReturn(orderId);
		when(db.queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId))
			.thenReturn(orderId);
		when(db.queryForObject("select id from payments where id=? for update", UUID.class, paymentId))
			.thenReturn(paymentId);
		when(db.queryForMap(anyString(), eq(paymentId))).thenReturn(projection);
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, mock(AuditEventRepository.class),
			mock(NotificationWriter.class), Clock.systemUTC());

		assertThat(service.paymentForUpdate(paymentId)).isSameAs(projection);

		InOrder order = inOrder(db);
		order.verify(db).queryForObject("select order_id from payments where id=?", UUID.class, paymentId);
		order.verify(db).queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId);
		order.verify(db).queryForObject("select id from payments where id=? for update", UUID.class, paymentId);
		ArgumentCaptor<String> projectionSql = ArgumentCaptor.forClass(String.class);
		order.verify(db).queryForMap(projectionSql.capture(), eq(paymentId));
		assertThat(projectionSql.getValue()).contains("from payments p join membership_orders o")
			.doesNotContain("for update");
		verifyNoMoreInteractions(db);
	}

	@Test
	void paymentForUpdatePreservesPaymentNotFoundBehavior() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select order_id from payments where id=?", UUID.class, paymentId))
			.thenThrow(new EmptyResultDataAccessException(1));
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, mock(AuditEventRepository.class),
			mock(NotificationWriter.class), Clock.systemUTC());

		assertThatThrownBy(() -> service.paymentForUpdate(paymentId))
			.isInstanceOf(PaymentException.class)
			.hasMessage("Payment was not found");
	}
}
