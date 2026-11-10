package com.scms.backend.course.waitlist;

import com.scms.backend.course.waitlist.CourseWaitlistService.Waiter;
import com.scms.backend.course.waitlist.CourseWaitlistService.WaitlistEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseWaitlistRepository {

	int joinInsertCourseWaitlists(UUID id, UUID runId, UUID memberId, Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.update("insert into course_waitlists(id,course_run_id,member_account_id,status,created_at,updated_at) values (?,?,?,'WAITING',?,?)", id, runId, memberId, nowTimestamp, nowTimestamp2);
	}

	int leaveUpdateCourseWaitlists(Timestamp nowTimestamp, UUID id) {
		return db.update("update course_waitlists set status='LEFT',updated_at=? where id=? and status in ('WAITING','INVITED')", nowTimestamp, id);
	}

	List<com.scms.backend.course.waitlist.CourseWaitlistResponse> mineFindRows(UUID memberId) {
		return db.query(selectWithPosition() + " where waitlist.member_account_id=? order by waitlist.created_at desc,waitlist.id desc", this::map, memberId);
	}

	List<UUID> acceptFindCourseWaitlists(UUID waitlistId, UUID memberId) {
		return db.query("select course_run_id from course_waitlists where id=? and member_account_id=?", (rs, row) -> rs.getObject(1, UUID.class), waitlistId, memberId);
	}

	Map<java.lang.String, java.lang.Object> lockInvitationForAcceptance(UUID waitlistId) {
		return db.queryForMap("select status,invitation_expires_at,accepted_order_id from course_waitlists where id=? for update", waitlistId);
	}

	int acceptUpdateCourseWaitlists(Timestamp nowTimestamp, UUID waitlistId) {
		return db.update("update course_waitlists set status='EXPIRED',updated_at=? where id=?", nowTimestamp, waitlistId);
	}

	int markInvitationAccepted(UUID value1, Timestamp nowTimestamp, UUID waitlistId) {
		return db.update("update course_waitlists set status='ACCEPTED',accepted_order_id=?,updated_at=? where id=?", value1, nowTimestamp, waitlistId);
	}

	List<UUID> promoteFindCourseWaitlists(UUID runId) {
		return db.queryForList("select member_account_id from course_waitlists where course_run_id=? and status='WAITING' order by created_at,id limit 1", UUID.class, runId);
	}

	int expireInvitationsBeforePromotion(Timestamp nowTimestamp, UUID runId, Timestamp nowTimestamp2) {
		return db.update("update course_waitlists set status='EXPIRED',updated_at=? where course_run_id=? and status='INVITED' and invitation_expires_at<=?", nowTimestamp, runId, nowTimestamp2);
	}

	int expireInvitationsDuringPromotion(Timestamp nowTimestamp, UUID runId, Timestamp nowTimestamp2) {
		return db.update("update course_waitlists set status='EXPIRED',updated_at=? where course_run_id=? and status='INVITED' and invitation_expires_at<=?", nowTimestamp, runId, nowTimestamp2);
	}

	int cancelWaitingAndInvitedForRun(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_waitlists set status='CANCELLED',updated_at=? where course_run_id=? and status in ('WAITING','INVITED')", nowTimestamp, runId);
	}

	int cancelWaitingCandidate(Timestamp nowTimestamp, UUID runId, UUID candidateMemberId) {
		return db.update("update course_waitlists set status='CANCELLED',updated_at=? where course_run_id=? and member_account_id=? and status='WAITING'", nowTimestamp, runId, candidateMemberId);
	}

	List<Waiter> lockNextWaitingCandidate(UUID runId) {
		return db.query("select id,member_account_id from course_waitlists where course_run_id=? and status='WAITING' order by created_at,id limit 1 for update skip locked", (rs, row) -> new Waiter(rs.getObject("id", UUID.class), rs.getObject("member_account_id", UUID.class)), runId);
	}

	List<Timestamp> promoteFindCourseSessions(UUID runId, Timestamp nowTimestamp) {
		return db.queryForList("select start_at from course_sessions where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED') and start_at>? order by start_at,id limit 1", Timestamp.class, runId, nowTimestamp);
	}

	int inviteWaitingCandidate(Timestamp nowTimestamp, Timestamp expiresAtTimestamp, Timestamp nowTimestamp2, UUID waitingId) {
		return db.update("update course_waitlists set status='INVITED',invited_at=?,invitation_expires_at=?,updated_at=? where id=? and status='WAITING'", nowTimestamp, expiresAtTimestamp, nowTimestamp2, waitingId);
	}

	List<WaitlistEntry> cancelForRunFindCourseWaitlists(UUID runId, UUID runId2) {
		return db.query("select id,member_account_id from course_waitlists where course_run_id=? and status in ('WAITING','INVITED') for update", (rs, row) -> new WaitlistEntry(rs.getObject("id", UUID.class), runId, rs.getObject("member_account_id", UUID.class)), runId2);
	}

	int cancelForRunUpdateCourseWaitlists(UUID runId) {
		return db.update("update course_waitlists set status='CANCELLED',updated_at=current_timestamp where course_run_id=? and status in ('WAITING','INVITED')", runId);
	}

	List<WaitlistEntry> cancelForMemberFindCourseWaitlists(UUID memberId, UUID memberId2) {
		return db.query("select id,course_run_id from course_waitlists where member_account_id=? and status in ('WAITING','INVITED') for update", (rs, row) -> new WaitlistEntry(rs.getObject("id", UUID.class), rs.getObject("course_run_id", UUID.class), memberId), memberId2);
	}

	int cancelForMemberUpdateCourseWaitlists(UUID memberId) {
		return db.update("update course_waitlists set status='CANCELLED',updated_at=current_timestamp where member_account_id=? and status in ('WAITING','INVITED')", memberId);
	}

	List<UUID> candidateRunIdsFindCourseWaitlists(Timestamp nowTimestamp, UUID afterRunId, UUID afterRunId2) {
		return db.queryForList("""
			select distinct course_run_id from course_waitlists
			where (status='WAITING' or (status='INVITED' and invitation_expires_at<=?))
			  and (cast(? as uuid) is null or course_run_id>?)
			order by course_run_id limit 100
			""", UUID.class, nowTimestamp, afterRunId, afterRunId2);
	}

	java.lang.Long usedSeatsFindCourseEnrollments(UUID runId, UUID runId2, Timestamp nowTimestamp, UUID runId3, Timestamp nowTimestamp2, UUID runId4, Timestamp nowTimestamp3) {
		return db.queryForObject("""
			select (select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))
			 + (select count(*) from course_reservations where course_run_id=? and status='HELD' and expires_at>?)
			 + (select count(*) from course_waitlists where course_run_id=? and status='INVITED' and invitation_expires_at>?)
			 + (select count(*) from course_transfer_holds where target_run_id=? and status='HELD' and expires_at>?)
			""", Long.class, runId, runId2, nowTimestamp, runId3, nowTimestamp2, runId4, nowTimestamp3);
	}

	java.lang.Boolean hasEnrollmentOrHoldFindCourseEnrollments(UUID memberId, UUID runId, UUID memberId2, UUID runId2, Timestamp nowTimestamp, UUID memberId3, UUID runId3, UUID memberId4, UUID runId4, Timestamp nowTimestamp2) {
		return db.queryForObject("""
			select exists(select 1 from course_enrollments where member_account_id=? and course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))
			 or exists(select 1 from course_reservations where member_account_id=? and course_run_id=?
				and status='HELD' and expires_at>?)
			 or exists(select 1 from course_waitlists where member_account_id=? and course_run_id=? and status in ('WAITING','INVITED'))
			 or exists(select 1 from course_transfer_holds where member_account_id=? and target_run_id=? and status='HELD' and expires_at>?)
			""", Boolean.class, memberId, runId, memberId2, runId2, nowTimestamp, memberId3, runId3, memberId4, runId4, nowTimestamp2);
	}

	List<UUID> activeIdFindCourseWaitlists(UUID memberId, UUID runId) {
		return db.queryForList("select id from course_waitlists where member_account_id=? and course_run_id=? and status in ('WAITING','INVITED') for update", UUID.class, memberId, runId);
	}

	List<com.scms.backend.course.waitlist.CourseWaitlistResponse> findFindRows(UUID id) {
		return db.query(selectWithPosition() + " where waitlist.id=?", this::map, id);
	}

	private CourseWaitlistResponse map(ResultSet rs, int row) throws SQLException {
		Timestamp invited = rs.getTimestamp("invited_at");
		Timestamp expires = rs.getTimestamp("invitation_expires_at");
		Integer position = rs.getObject("queue_position", Integer.class);
		return new CourseWaitlistResponse(rs.getObject("id", UUID.class), rs.getObject("course_run_id", UUID.class), rs.getString("course_name"), rs.getString("course_code"), rs.getString("run_code"), rs.getString("status"), position, invited == null ? null : invited.toInstant(), expires == null ? null : expires.toInstant(), rs.getTimestamp("created_at").toInstant());
	}

	private String selectWithPosition() {
		return """
			select waitlist.*,course.name course_name,course.code course_code,run.run_code,
				case when waitlist.status='WAITING' then (
					select count(*)::integer+1 from course_waitlists earlier
					where earlier.course_run_id=waitlist.course_run_id and earlier.status='WAITING'
					  and (earlier.created_at,earlier.id)<(waitlist.created_at,waitlist.id)
				) end as queue_position
			from course_waitlists waitlist
			join course_runs run on run.id=waitlist.course_run_id
			join courses course on course.id=run.course_id
			""";
	}

	private final JdbcTemplate db;

	CourseWaitlistRepository(JdbcTemplate db) {
		this.db = db;
	}
}
