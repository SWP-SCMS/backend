package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MembershipRefundMigrationContractTests {
	@Test
	void refundLedgerKeepsOriginalCommerceRowsAndAllowsOnlyOneSuccessfulRefundPerPayment() throws Exception {
		String migration = Files.readString(Path.of("src/main/resources/db/migration/V38__membership_refunds.sql"));

		assertThat(migration).contains("create table refund_requests");
		assertThat(migration).contains("fk_refund_payment_order", "fk_refund_order_member", "fk_refund_membership_member", "fk_refund_receipt_context");
		assertThat(migration).contains("payment_id uuid not null", "membership_id uuid not null");
		assertThat(migration).contains("requested_amount numeric(19,0) not null");
		assertThat(migration).contains("approved_amount numeric(19,0)");
		assertThat(migration).contains("version bigint not null default 0");
		assertThat(migration).contains("REQUESTED", "FIRST_APPROVED", "CASH_DISBURSED", "COMPLETED", "REJECTED", "CANCELLED");
		assertThat(migration).contains("where status not in ('REJECTED','CANCELLED')");
		assertThat(migration).contains("create table credit_notes", "fk_credit_note_receipt_context", "trg_credit_notes_immutable");
	}
}
