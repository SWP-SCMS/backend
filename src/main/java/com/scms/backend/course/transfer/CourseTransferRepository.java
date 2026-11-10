package com.scms.backend.course.transfer;

import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer;
import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferRequest;
import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferTarget;
import com.scms.backend.course.CourseTransferException;
import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseTransferRepository {

	private final JdbcTemplate db;

	CourseTransferRepository(JdbcTemplate db) {
		this.db = db;
	}

	BigInteger unusedCredit(UUID enrollmentId, Instant now) {
		List<CourseTransferPolicy.UnitAllocation> units = db.query("""
			select s.start_at,es.allocation_vnd from course_enrollment_sessions es
			join course_sessions s on s.id=es.course_session_id where es.course_enrollment_id=? and not exists (select 1 from course_refund_unit_claims c where c.enrollment_session_id=es.id)
			""", (rs, n) -> new CourseTransferPolicy.UnitAllocation(rs.getTimestamp(1).toInstant(), rs.getBigDecimal(2).toBigIntegerExact()), enrollmentId);
		return CourseTransferPolicy.unusedCredit(units, now);
	}

	long usedSeats(UUID runId, Instant now) {
		return usedSeatsExcludingOffer(runId, null, now);
	}

	long usedSeatsExcludingOffer(UUID runId, UUID excludedOfferId, Instant now) {
		return db.queryForObject("""
			select (select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))
			 +(select count(*) from course_reservations where course_run_id=? and status='HELD' and expires_at>?)
			 +(select count(*) from course_waitlists where course_run_id=? and status='INVITED' and invitation_expires_at>?)
			 +(select count(*) from course_transfer_holds where target_run_id=? and status='HELD' and expires_at>?
			   and (?::uuid is null or transfer_offer_id<>?))
			""", Long.class, runId, runId, Timestamp.from(now), runId, Timestamp.from(now), runId, Timestamp.from(now), excludedOfferId, excludedOfferId);
	}

	boolean hasScheduleConflict(UUID memberId, UUID sourceRunId, UUID targetRunId) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists(select 1 from course_sessions target
			 join course_enrollment_sessions existing_session on existing_session.disposition='ENROLLED'
			 join course_enrollments existing on existing.id=existing_session.course_enrollment_id
			 join course_sessions existing_time on existing_time.id=existing_session.course_session_id
			 where target.course_run_id=? and target.status in ('SCHEDULED','MAKEUP_SCHEDULED')
			 and existing.member_account_id=? and existing.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') and existing.course_run_id<>?
			 and existing_time.start_at<target.end_at and existing_time.end_at>target.start_at)
			""", Boolean.class, targetRunId, memberId, sourceRunId));
	}

	TransferTarget target(ResultSet rs, Instant now) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		Timestamp next = rs.getTimestamp("next_session_at");
		return new TransferTarget(id, rs.getString("run_code"), rs.getString("course_code"), rs.getString("course_name"), rs.getBigDecimal("tuition_vnd").toBigIntegerExact(), rs.getInt("capacity"), usedSeats(id, now), rs.getTimestamp("registration_closes_at").toInstant(), next == null ? null : next.toInstant());
	}

	void lockRuns(UUID first, UUID second) {
		for (UUID id : List.of(first, second).stream().distinct().sorted().toList()) db.queryForObject("select id from course_runs where id=? for update", UUID.class, id);
	}

	int capacity(UUID runId) {
		return db.queryForObject("select capacity from course_runs where id=?", Integer.class, runId);
	}

	boolean activeEnrollmentExists(UUID memberId, UUID runId) {
		return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from course_enrollments where member_account_id=? and course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))", Boolean.class, memberId, runId));
	}

	TransferRequest lockRequest(UUID id) {
		return db.query(requestSelect() + " where tr.id=? for update of tr", this::mapRequest, id).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferRequest findRequest(UUID id) {
		return db.query(requestSelect() + " where tr.id=?", this::mapRequest, id).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferRequest findRequestByResponse(UUID id) {
		return db.query(requestSelect() + " where tr.schedule_change_response_id=?", this::mapRequest, id).stream().findFirst().orElse(null);
	}

	TransferOffer lockOffer(UUID memberId, UUID id) {
		return db.query(offerSelect() + " where transfer.id=? and transfer.member_account_id=? for update of transfer", this::mapOffer, id, memberId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferOffer findOffer(UUID id) {
		return db.query(offerSelect() + " where transfer.id=?", this::mapOffer, id).stream().findFirst().orElseThrow(CourseTransferException::notFound);
	}

	TransferOffer findOfferByRequest(UUID id) {
		return db.query(offerSelect() + " where transfer.transfer_request_id=?", this::mapOffer, id).stream().findFirst().orElse(null);
	}

	CourseEnrollmentResponse targetEnrollment(UUID offerId) {
		Map<String, Object> row = rows("select id,course_order_id,member_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at from course_enrollments where source_transfer_offer_id=?", offerId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		return new CourseEnrollmentResponse(uuid(row, "id"), uuid(row, "course_order_id"), uuid(row, "member_account_id"), uuid(row, "course_run_id"), amount(row, "amount_paid_vnd_snapshot"), number(row, "total_units_snapshot"), instant(row, "enrolled_at"));
	}

	String requestSelect() {
		return "select tr.*,member.id member_id,mp.member_code member_code_display,member.full_name member_name_display," + "source_course.id source_course_id_display,source_course.code source_course_code_display,source_course.name source_course_name_display," + "source_run.run_code source_run_code_display,source_run.status source_course_run_status_display," + "target_course.id target_course_id_display,target_course.code target_course_code_display,target_course.name target_course_name_display," + "target_run.run_code target_run_code_display,target_run.status target_course_run_status_display " + "from course_transfer_requests tr join accounts member on member.id=tr.member_account_id " + "left join member_profiles mp on mp.account_id=member.id " + "left join course_runs source_run on source_run.id=tr.source_run_id " + "left join courses source_course on source_course.id=source_run.course_id " + "left join course_runs target_run on target_run.id=tr.target_run_id " + "left join courses target_course on target_course.id=target_run.course_id";
	}

	String offerSelect() {
		return "select transfer.*,member.id member_id,mp.member_code member_code_display,member.full_name member_name_display," + "source_course.id source_course_id_display,source_course.code source_course_code_display,source_course.name source_course_name_display," + "source_run.run_code source_run_code_display,source_run.status source_course_run_status_display," + "target_course.id target_course_id_display,target_course.code target_course_code_display,target_course.name target_course_name_display," + "target_run.run_code target_run_code_display,target_run.status target_course_run_status_display " + "from course_transfer_offers transfer join accounts member on member.id=transfer.member_account_id " + "left join member_profiles mp on mp.account_id=member.id " + "left join course_runs source_run on source_run.id=transfer.source_run_id " + "left join courses source_course on source_course.id=source_run.course_id " + "left join course_runs target_run on target_run.id=transfer.target_run_id " + "left join courses target_course on target_course.id=target_run.course_id";
	}

	TransferRequest mapRequest(ResultSet rs, int n) throws SQLException {
		return new TransferRequest(rs.getObject("id", UUID.class), rs.getObject("schedule_change_response_id", UUID.class), rs.getObject("source_enrollment_id", UUID.class), rs.getObject("source_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getString("status"), rs.getObject("target_run_id", UUID.class), rs.getString("reason"), rs.getTimestamp("requested_at").toInstant(), rs.getObject("decided_by_account_id", UUID.class), rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(), rs.getObject("member_id", UUID.class), rs.getString("member_code_display"), rs.getString("member_name_display"), rs.getObject("source_course_id_display", UUID.class), rs.getString("source_course_code_display"), rs.getString("source_course_name_display"), rs.getString("source_run_code_display"), rs.getString("source_course_run_status_display"), rs.getObject("target_course_id_display", UUID.class), rs.getString("target_course_code_display"), rs.getString("target_course_name_display"), rs.getString("target_run_code_display"), rs.getString("target_course_run_status_display"));
	}

	TransferOffer mapOffer(ResultSet rs, int n) throws SQLException {
		return new TransferOffer(rs.getObject("id", UUID.class), rs.getObject("transfer_request_id", UUID.class), rs.getObject("source_enrollment_id", UUID.class), rs.getObject("source_run_id", UUID.class), rs.getObject("target_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getBigDecimal("credit_vnd_snapshot").toBigIntegerExact(), rs.getBigDecimal("target_tuition_vnd_snapshot").toBigIntegerExact(), rs.getBigDecimal("difference_vnd_snapshot").toBigIntegerExact(), rs.getTimestamp("expires_at").toInstant(), rs.getString("status"), rs.getObject("difference_order_id", UUID.class), rs.getTimestamp("accepted_at") == null ? null : rs.getTimestamp("accepted_at").toInstant(), rs.getObject("member_id", UUID.class), rs.getString("member_code_display"), rs.getString("member_name_display"), rs.getObject("source_course_id_display", UUID.class), rs.getString("source_course_code_display"), rs.getString("source_course_name_display"), rs.getString("source_run_code_display"), rs.getString("source_course_run_status_display"), rs.getObject("target_course_id_display", UUID.class), rs.getString("target_course_code_display"), rs.getString("target_course_name_display"), rs.getString("target_run_code_display"), rs.getString("target_course_run_status_display"));
	}

	List<Map<String, Object>> rows(String sql, Object... args) {
		return db.queryForList(sql, args);
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return (UUID) row.get(key);
	}

	static int number(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).intValue();
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return new BigDecimal(row.get(key).toString()).toBigIntegerExact();
	}

	static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? null : value instanceof Instant i ? i : ((Timestamp) value).toInstant();
	}

	java.lang.String completeTransferFindCourseEnrollments(UUID sourceEnrollmentId) {
		return db.queryForObject("select lifecycle_status from course_enrollments where id=? for update", String.class, sourceEnrollmentId);
	}

	UUID completeTransferFindCourseReservations(UUID orderId) {
		return db.queryForObject("select id from course_reservations where course_order_id=?", UUID.class, orderId);
	}

	UUID completeTransferFindCourseOrders(UUID orderId) {
		return db.queryForObject("select actor_account_id from course_orders where id=?", UUID.class, orderId);
	}

	java.lang.Integer completeTransferFindCourseRuns(UUID targetRunId) {
		return db.queryForObject("select total_units from course_runs where id=?", Integer.class, targetRunId);
	}

	int completeTransferInsertCourseEnrollments(UUID enrollmentId, UUID orderId, UUID reservationId, UUID paymentId, UUID memberAccountId, UUID initiatorId, UUID actorId, UUID targetRunId, BigInteger targetTuitionVnd, int totalUnits, Timestamp nowTimestamp, java.lang.String value12, UUID offerId, BigInteger appliedCredit, BigInteger cashPaid) {
		return db.update("""
			insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,member_account_id,
			 actor_account_id,processed_by_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,
			 lifecycle_status,source_transfer_offer_id,credit_applied_vnd,cash_paid_vnd)
			 values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			""", enrollmentId, orderId, reservationId, paymentId, memberAccountId, initiatorId, actorId, targetRunId, targetTuitionVnd, totalUnits, nowTimestamp, value12, offerId, appliedCredit, cashPaid);
	}

	List<Map<java.lang.String, java.lang.Object>> completeTransferRowsCourseSessions(UUID targetRunId) {
		return rows("select id,unit_number,start_at from course_sessions where course_run_id=? and is_makeup=false order by unit_number,id", targetRunId);
	}

	int completeTransferInsertCourseEnrollmentSessions(UUID randomUUID, UUID enrollmentId, UUID id, UUID targetRunId, int unitNumber, BigInteger allocation, java.lang.Object value7) {
		return db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,unit_number,allocation_vnd,disposition) values(?,?,?,?,?,?,?)", randomUUID, enrollmentId, id, targetRunId, unitNumber, allocation, value7);
	}

	int completeTransferUpdateCourseEnrollments(UUID sourceEnrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='TRANSFERRED' where id=? and lifecycle_status='ACTIVE'", sourceEnrollmentId);
	}

	int completeTransferUpdateCourseReservations(Timestamp nowTimestamp, UUID sourceEnrollmentId) {
		return db.update("update course_reservations set status='TRANSFERRED',updated_at=? where id=(select course_reservation_id from course_enrollments where id=?) and status='ENROLLED'", nowTimestamp, sourceEnrollmentId);
	}

	int completeTransferUpdateCourseTransferHolds(Timestamp nowTimestamp, UUID offerId) {
		return db.update("update course_transfer_holds set status='CONSUMED',released_at=? where transfer_offer_id=? and status='HELD'", nowTimestamp, offerId);
	}

	int completeTransferUpdateCourseTransferOffers(Timestamp nowTimestamp, UUID offerId) {
		return db.update("update course_transfer_offers set status='COMPLETED',completed_at=? where id=?", nowTimestamp, offerId);
	}

	int completeTransferUpdateCourseTransferRequests(Timestamp nowTimestamp, UUID requestId) {
		return db.update("update course_transfer_requests set status='COMPLETED',completed_at=? where id=?", nowTimestamp, requestId);
	}

	int completeTransferInsertCourseRefundObligations(UUID randomUUID, UUID sourceEnrollmentId, UUID sourceRunId, BigInteger value4, UUID actorId) {
		return db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,
			 requester_account_id,request_reason)
			values(?,?,?,'TRANSFER_CREDIT_EXCESS',?,?,'Unused Course transfer credit refund')
			on conflict(course_enrollment_id,source_type,source_event_id) do nothing
			""", randomUUID, sourceEnrollmentId, sourceRunId, value4, actorId);
	}

	List<UUID> completeTransferFindAccounts() {
		return db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE'", UUID.class);
	}

	List<Map<java.lang.String, java.lang.Object>> requestFromScheduleChangeRowsCourseScheduleChangeResponses(UUID responseId, UUID memberId) {
		return rows("""
			select e.id source_enrollment_id,e.course_run_id source_run_id,e.lifecycle_status
			from course_schedule_change_responses response
			join course_enrollment_sessions es on es.id=response.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			where response.id=? and e.member_account_id=?
			""", responseId, memberId);
	}

	int requestFromScheduleChangeInsertCourseTransferRequests(UUID requestId, UUID responseId, UUID sourceEnrollmentId, UUID sourceRunId, UUID memberId, UUID actorId, Timestamp requestedAtTimestamp) {
		return db.update("""
			insert into course_transfer_requests(id,schedule_change_response_id,source_enrollment_id,source_run_id,
				member_account_id,requester_account_id,requested_at) values(?,?,?,?,?,?,?)
			""", requestId, responseId, sourceEnrollmentId, sourceRunId, memberId, actorId, requestedAtTimestamp);
	}

	List<UUID> requestFromScheduleChangeFindAccounts() {
		return db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE'", UUID.class);
	}

	List<com.scms.backend.course.transfer.CourseTransferApiModels.TransferRequest> managerQueueFindRows() {
		return db.query(requestSelect() + " order by tr.requested_at,tr.id", this::mapRequest);
	}

	List<com.scms.backend.course.transfer.CourseTransferApiModels.TransferTarget> eligibleTargetsFindCourseSessions(Instant now, Timestamp nowTimestamp, UUID sourceRunId, Timestamp nowTimestamp2, Timestamp nowTimestamp3) {
		return db.query("""
			select r.id,r.run_code,c.code course_code,c.name course_name,r.tuition_vnd,r.capacity,
				r.registration_closes_at,(select min(s.start_at) from course_sessions s where s.course_run_id=r.id
					and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at>?) next_session_at
			from course_runs r join courses c on c.id=r.course_id
			where r.id<>? and r.status='OPEN_FOR_REGISTRATION' and c.status='PUBLISHED'
			  and r.registration_opens_at<=? and r.registration_closes_at>?
			order by r.registration_closes_at,r.id
			""", (rs, n) -> target(rs, now), nowTimestamp, sourceRunId, nowTimestamp2, nowTimestamp3);
	}

	List<Map<java.lang.String, java.lang.Object>> approveRowsCourseSessions(Timestamp nowTimestamp, UUID targetRunId) {
		return rows("""
			select r.id,r.tuition_vnd,r.capacity,r.registration_opens_at,r.registration_closes_at,r.status,c.status course_status,
			 (select min(s.start_at) from course_sessions s where s.course_run_id=r.id
			  and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at>?) next_session_at
			from course_runs r join courses c on c.id=r.course_id where r.id=?
			""", nowTimestamp, targetRunId);
	}

	java.lang.String approveFindCourseEnrollments(UUID sourceEnrollmentId) {
		return db.queryForObject("select lifecycle_status from course_enrollments where id=? for update", String.class, sourceEnrollmentId);
	}

	List<Map<java.lang.String, java.lang.Object>> findTargetRunSessionTimes(UUID targetRunId) {
		return rows("select start_at,end_at from course_sessions where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED')", targetRunId);
	}

	int approveInsertCourseTransferOffers(UUID offerId, UUID requestId, UUID sourceEnrollmentId, UUID sourceRunId, UUID targetRunId, UUID memberAccountId, BigInteger credit, BigInteger tuition, BigInteger value9, Timestamp expiresAtTimestamp, Timestamp nowTimestamp) {
		return db.update("""
			insert into course_transfer_offers(id,transfer_request_id,source_enrollment_id,source_run_id,target_run_id,
			 member_account_id,credit_vnd_snapshot,target_tuition_vnd_snapshot,difference_vnd_snapshot,expires_at,issued_at)
			 values(?,?,?,?,?,?,?,?,?,?,?)
			""", offerId, requestId, sourceEnrollmentId, sourceRunId, targetRunId, memberAccountId, credit, tuition, value9, expiresAtTimestamp, nowTimestamp);
	}

	int approveInsertCourseTransferHolds(UUID randomUUID, UUID offerId, UUID targetRunId, UUID memberAccountId, Timestamp expiresAtTimestamp, Timestamp nowTimestamp) {
		return db.update("insert into course_transfer_holds(id,transfer_offer_id,target_run_id,member_account_id,expires_at,created_at) values(?,?,?,?,?,?)", randomUUID, offerId, targetRunId, memberAccountId, expiresAtTimestamp, nowTimestamp);
	}

	int approveUpdateCourseTransferRequests(UUID targetRunId, java.lang.String normalizedReason, UUID managerId, Timestamp nowTimestamp, UUID requestId) {
		return db.update("update course_transfer_requests set status='OFFERED',target_run_id=?,reason=?,decided_by_account_id=?,decided_at=? where id=?", targetRunId, normalizedReason, managerId, nowTimestamp, requestId);
	}

	int rejectUpdateCourseTransferRequests(java.lang.String normalized, UUID managerId, Timestamp nowTimestamp, UUID requestId) {
		return db.update("update course_transfer_requests set status='REJECTED',reason=?,decided_by_account_id=?,decided_at=? where id=?", normalized, managerId, nowTimestamp, requestId);
	}

	List<com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer> memberOffersFindRows(UUID memberId) {
		return db.query(offerSelect() + " where transfer.member_account_id=? order by transfer.issued_at desc,transfer.id", this::mapOffer, memberId);
	}

	java.lang.String acceptFindCourseTransferOffers(UUID offerId) {
		return db.queryForObject("select acceptance_idempotency_key from course_transfer_offers where id=?", String.class, offerId);
	}

	int acceptUpdateCourseTransferOffers(java.lang.String key, UUID orderId, Timestamp nowTimestamp, UUID offerId) {
		return db.update("update course_transfer_offers set status='PENDING_PAYMENT',acceptance_idempotency_key=?,difference_order_id=?,accepted_at=? where id=?", key, orderId, nowTimestamp, offerId);
	}

	int recordOfferAcceptance(java.lang.String key, Timestamp nowTimestamp, UUID offerId) {
		return db.update("update course_transfer_offers set acceptance_idempotency_key=?,accepted_at=? where id=?", key, nowTimestamp, offerId);
	}

	List<com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer> completePaidDifferenceFindOf(UUID orderId) {
		return db.query(offerSelect() + " where transfer.difference_order_id=? for update of transfer", this::mapOffer, orderId);
	}

	List<com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer> expireDueFindOf(Timestamp nowTimestamp) {
		return db.query(offerSelect() + " where transfer.status in ('OFFERED','PENDING_PAYMENT') and transfer.expires_at<=? order by transfer.id for update of transfer skip locked", this::mapOffer, nowTimestamp);
	}

	int createDifferenceOrderInsertCourseOrders(UUID orderId, java.lang.String reference, UUID memberId, UUID actorId, UUID targetRunId, BigInteger differenceVnd, java.lang.String value7, Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,tuition_vnd_snapshot,
			 currency_code,status,idempotency_key,created_at,updated_at) values(?,?,?,?,?,?,'VND','PENDING_PAYMENT',?,?,?)
			""", orderId, reference, memberId, actorId, targetRunId, differenceVnd, value7, nowTimestamp, nowTimestamp2);
	}

	int createDifferenceOrderInsertCourseReservations(UUID reservationId, UUID orderId, UUID memberId, UUID targetRunId, Timestamp expiresTimestamp, Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,created_at,updated_at) values(?,?,?,?,'HELD',?,?,?)", reservationId, orderId, memberId, targetRunId, expiresTimestamp, nowTimestamp, nowTimestamp2);
	}

	int createDifferenceOrderUpdateCourseTransferHolds(Timestamp expiresTimestamp, Timestamp nowTimestamp, UUID offerId) {
		return db.update("update course_transfer_holds set status='RELEASED',expires_at=?,released_at=? where transfer_offer_id=? and status='HELD'", expiresTimestamp, nowTimestamp, offerId);
	}

	int expireOfferUpdateCourseTransferOffers(UUID offerId) {
		return db.update("update course_transfer_offers set status='EXPIRED' where id=? and status in ('OFFERED','PENDING_PAYMENT')", offerId);
	}

	int expireOfferUpdateCourseTransferHolds(Timestamp nowTimestamp, UUID offerId) {
		return db.update("update course_transfer_holds set status='RELEASED',released_at=? where transfer_offer_id=? and status='HELD'", nowTimestamp, offerId);
	}

	int expireOfferUpdateCourseTransferRequests(UUID requestId) {
		return db.update("update course_transfer_requests set status='EXPIRED' where id=? and status='OFFERED'", requestId);
	}

	int expireOfferUpdateCourseReservations(Timestamp nowTimestamp, UUID differenceOrderId) {
		return db.update("update course_reservations set status='EXPIRED',updated_at=? where course_order_id=? and status='HELD'", nowTimestamp, differenceOrderId);
	}

	int expireOfferUpdateCourseOrders(Timestamp nowTimestamp, UUID differenceOrderId) {
		return db.update("update course_orders set status='EXPIRED',updated_at=? where id=? and status='PENDING_PAYMENT'", nowTimestamp, differenceOrderId);
	}

	int expireOfferUpdateCoursePayments(Timestamp nowTimestamp, UUID differenceOrderId) {
		return db.update("update course_payments set status='FAILED',updated_at=? where course_order_id=? and status='PENDING'", nowTimestamp, differenceOrderId);
	}

	int expireDueForRunUpdateCourseTransferHolds(Timestamp nowTimestamp, UUID runId, Timestamp nowTimestamp2) {
		return db.update("update course_transfer_holds set status='RELEASED',released_at=? where target_run_id=? and status='HELD' and expires_at<=?", nowTimestamp, runId, nowTimestamp2);
	}

	int expireDueForRunUpdateCourseTransferRequests(UUID runId, Timestamp nowTimestamp) {
		return db.update("update course_transfer_requests set status='EXPIRED' where status='OFFERED' and id in " + "(select transfer_request_id from course_transfer_offers where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT') and expires_at<=?)", runId, nowTimestamp);
	}

	int expireDueForRunUpdateCourseTransferOffers(UUID runId, Timestamp nowTimestamp) {
		return db.update("update course_transfer_offers set status='EXPIRED' where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT') and expires_at<=?", runId, nowTimestamp);
	}
}
