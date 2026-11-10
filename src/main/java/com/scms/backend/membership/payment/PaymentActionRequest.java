package com.scms.backend.membership.payment;

import java.math.BigInteger;

public record PaymentActionRequest(ReconciliationStatus status, BigInteger receivedAmount, String transferContent, String providerTransactionId, String evidence, String reason) {

	public enum ReconciliationStatus {

		PAID, FAILED
	}
}
