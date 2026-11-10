package com.scms.backend.membership.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.scms.backend.membership.PaymentException;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class CourseReconciliationValidationTests {

	@Test
	void requiresEvidenceReasonKeyAndExactPositiveMoneyInputs() {
		var paid = new PaymentActionRequest(PaymentActionRequest.ReconciliationStatus.PAID, BigInteger.ONE, "COR-X", "tx", "proof", "reason");
		assertThatCode(() -> CourseReconciliationService.validate("retry", paid)).doesNotThrowAnyException();
		for (String key : new String[] { null, "", " ", "a".repeat(129) }) assertThatThrownBy(() -> CourseReconciliationService.validate(key, paid)).isInstanceOf(PaymentException.class);
		assertThatThrownBy(() -> CourseReconciliationService.validate("key", null)).isInstanceOf(PaymentException.class);
		for (BigInteger amount : new BigInteger[] { null, BigInteger.ZERO, BigInteger.valueOf(-1) }) assertThatThrownBy(() -> CourseReconciliationService.validate("key", new PaymentActionRequest(paid.status(), amount, "COR-X", "tx", "proof", "reason"))).isInstanceOf(PaymentException.class);
		for (String evidence : new String[] { null, " ", "x".repeat(4001) }) assertThatThrownBy(() -> CourseReconciliationService.validate("key", new PaymentActionRequest(paid.status(), BigInteger.ONE, "COR-X", "tx", evidence, "reason"))).isInstanceOf(PaymentException.class);
	}

	@Test
	void failureStillRequiresEvidenceButNotAnInventedBankTransaction() {
		assertThatCode(() -> CourseReconciliationService.validate("key", new PaymentActionRequest(PaymentActionRequest.ReconciliationStatus.FAILED, null, null, null, "checked", "expired"))).doesNotThrowAnyException();
	}
}
