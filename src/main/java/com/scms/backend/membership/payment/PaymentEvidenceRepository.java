package com.scms.backend.membership.payment;

import com.scms.backend.membership.payment.PaymentEvidenceService.Evidence;
import com.scms.backend.membership.payment.PaymentEvidenceService.Image;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PaymentEvidenceRepository {

	java.util.List<com.scms.backend.membership.payment.PaymentEvidenceService.Evidence> listFindPaymentEvidenceImages(java.util.UUID paymentId) {
		return db.query("select id,created_at from payment_evidence_images where payment_id=? order by created_at,id", (rs, n) -> new Evidence(rs.getObject("id", UUID.class), rs.getTimestamp("created_at").toInstant()), paymentId);
	}

	java.util.List<java.lang.String> uploadFindPayments(java.util.UUID paymentId) {
		return db.queryForList("select status from payments where id=? for update", String.class, paymentId);
	}

	java.util.List<com.scms.backend.membership.payment.PaymentEvidenceService.Evidence> findDuplicateEvidence(java.util.UUID paymentId, java.lang.String digest) {
		return db.query("select id,created_at from payment_evidence_images where payment_id=? and sha256=?", (rs, n) -> new Evidence(rs.getObject("id", UUID.class), rs.getTimestamp("created_at").toInstant()), paymentId, digest);
	}

	java.lang.Integer countEvidence(java.util.UUID paymentId) {
		return db.queryForObject("select count(*) from payment_evidence_images where payment_id=?", Integer.class, paymentId);
	}

	com.scms.backend.membership.payment.PaymentEvidenceService.Evidence insertEvidence(java.util.UUID id, java.util.UUID paymentId, java.util.UUID actor, java.lang.String contentType, java.lang.String digest, byte[] bytes) {
		return db.queryForObject("""
			insert into payment_evidence_images(id,payment_id,uploaded_by,content_type,sha256,image_data)
			values (?,?,?,?,?,?) returning id,created_at
			""", (rs, n) -> new Evidence(rs.getObject("id", UUID.class), rs.getTimestamp("created_at").toInstant()), id, paymentId, actor, contentType, digest, bytes);
	}

	java.util.List<com.scms.backend.membership.payment.PaymentEvidenceService.Image> readFindPaymentEvidenceImages(java.util.UUID evidenceId, java.util.UUID paymentId) {
		return db.query("select content_type,image_data from payment_evidence_images where id=? and payment_id=?", (rs, n) -> new Image(rs.getString("content_type"), rs.getBytes("image_data")), evidenceId, paymentId);
	}

	private final JdbcTemplate db;

	PaymentEvidenceRepository(JdbcTemplate db) {
		this.db = db;
	}
}
