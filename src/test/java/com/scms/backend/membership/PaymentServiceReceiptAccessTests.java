package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PaymentServiceReceiptAccessTests {

	private final UUID receptionistId = UUID.randomUUID();
	private final UUID managerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID orderId = UUID.randomUUID();
	private final UUID paymentId = UUID.randomUUID();
	private final UUID receiptId = UUID.randomUUID();
	private final Instant issuedAt = Instant.parse("2026-10-04T16:00:00Z");
	private JdbcTemplate db;
	private PaymentFulfillmentService fulfillment;
	private PaymentService service;

	@BeforeEach
	void setUp() {
		db = mock(JdbcTemplate.class);
		fulfillment = mock(PaymentFulfillmentService.class);
		AccountRepository accounts = mock(AccountRepository.class);
		Account receptionist = new Account(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE,
			"Receptionist", "0900000000", "receptionist@example.test", LocalDate.of(1990, 1, 1), "hash");
		when(accounts.findById(receptionistId)).thenReturn(Optional.of(receptionist));
		when(accounts.existsByIdAndRoleAndStatus(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		service = new PaymentService(db, accounts, mock(AuditEventRepository.class), fulfillment,
			Clock.systemUTC());
	}

	@Test
	void receptionistCanReadBankTransferReceipt() {
		when(db.queryForMap(anyString(), eq(receiptId))).thenReturn(Map.of(
			"id", receiptId,
			"receipt_number", "RC-001",
			"payment_id", paymentId,
			"order_id", orderId,
			"offer_name_snapshot", "Monthly Basic",
			"member_account_id", memberId,
			"amount_snapshot", BigDecimal.valueOf(12000),
			"currency_code_snapshot", "VND",
			"payment_method_snapshot", "BANK_TRANSFER",
			"issued_at", Timestamp.from(issuedAt)
		));

		ReceiptResponse response = service.receipt(receptionistId, receiptId);

		assertThat(response.receiptId()).isEqualTo(receiptId);
		assertThat(response.paymentMethod()).isEqualTo("BANK_TRANSFER");
		assertThat(response.offerName()).isEqualTo("Monthly Basic");
	}

	@Test
	void receptionistCanReadBankTransferPaymentResult() {
		when(fulfillment.payment(paymentId)).thenReturn(Map.of(
			"payment_id", paymentId,
			"order_id", orderId,
			"member_account_id", memberId,
			"payment_method", "BANK_TRANSFER",
			"payment_status", "PENDING"
		));

		PaymentResultResponse response = service.result(receptionistId, paymentId);

		assertThat(response.paymentId()).isEqualTo(paymentId);
		assertThat(response.method()).isEqualTo("BANK_TRANSFER");
	}

	@Test
	void receptionistCanListCashAndBankTransferReceiptsByMemberCode() {
		ReceiptResponse cash = new ReceiptResponse(UUID.randomUUID(), "RC-CASH", UUID.randomUUID(),
			UUID.randomUUID(), "Monthly Basic", memberId, BigDecimal.valueOf(12000).toBigIntegerExact(), "VND",
			"CASH", issuedAt);
		ReceiptResponse bank = new ReceiptResponse(UUID.randomUUID(), "RC-BANK", UUID.randomUUID(),
			UUID.randomUUID(), "Monthly Basic", memberId, BigDecimal.valueOf(12000).toBigIntegerExact(), "VND",
			"BANK_TRANSFER", issuedAt.minusSeconds(60));
		when(db.queryForObject(anyString(), eq(UUID.class), eq("MB-100001"))).thenReturn(memberId);
		doReturn(List.of(cash, bank)).when(db).query(anyString(),
			org.mockito.ArgumentMatchers.<RowMapper<ReceiptResponse>>any(), eq(memberId));

		List<ReceiptResponse> response = service.receiptsForReceptionist(receptionistId, " MB-100001 ");

		assertThat(response).extracting(ReceiptResponse::paymentMethod)
			.containsExactly("CASH", "BANK_TRANSFER");
	}

	@Test
	void managerCanListReceiptsByMemberCode() {
		when(db.queryForObject(anyString(), eq(UUID.class), eq("MB-100001"))).thenReturn(memberId);
		doReturn(List.of()).when(db).query(anyString(),
			org.mockito.ArgumentMatchers.<RowMapper<ReceiptResponse>>any(), eq(memberId));

		assertThat(service.receiptsForReceptionist(managerId, "MB-100001")).isEmpty();
	}
}
