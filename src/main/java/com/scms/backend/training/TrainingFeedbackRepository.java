package com.scms.backend.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingFeedbackRepository extends JpaRepository<TrainingFeedback, UUID> {

	@Query("""
		select f as feedback, r.classSessionId as classSessionId
		from TrainingFeedback f
		left join TrainingResult r on r.id = f.trainingResultId
		where f.memberAccountId = :memberId
		order by f.createdAt desc, f.id desc
		""")
	List<MemberFeedbackHistoryView> findHistoryByMemberAccountId(@Param("memberId") UUID memberId);

	@Query(value = """
		select f as feedback, r.classSessionId as classSessionId
		from TrainingFeedback f
		left join TrainingResult r on r.id = f.trainingResultId
		where f.memberAccountId = :memberId
		order by f.createdAt desc, f.id desc
		""", countQuery = "select count(f) from TrainingFeedback f where f.memberAccountId = :memberId")
	Page<MemberFeedbackHistoryView> findHistoryByMemberAccountId(@Param("memberId") UUID memberId,
			org.springframework.data.domain.Pageable pageable);

	@Query(value = """
		select * from feedback
		where training_result_id = :resultId
		  and member_account_id = :memberId
		  and coach_assignment_id is null
		  and training_plan_version_id is null
		  and legacy_duplicate = false
		for update
		""", nativeQuery = true)
	Optional<TrainingFeedback> findSessionByTrainingResultIdAndMemberAccountIdForUpdate(
			@Param("resultId") UUID resultId, @Param("memberId") UUID memberId);

	@Query(value = """
		select f.* from feedback f
		join training_results r on r.id = f.training_result_id
		where f.member_account_id = :memberId
		  and f.coach_assignment_id is null
		  and f.training_plan_version_id is null
		  and f.legacy_duplicate = false
		  and r.class_session_id = :sessionId
		  and r.coach_assignment_id is null
		  and r.legacy_duplicate = false
		""", nativeQuery = true)
	Optional<TrainingFeedback> findSessionByClassSessionIdAndMemberAccountId(
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select f from TrainingFeedback f
		where f.id = :id
		  and f.memberAccountId = :memberId
		  and f.coachAssignmentId is not null
		""")
	Optional<TrainingFeedback> findPersonalByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select f from TrainingFeedback f, TrainingResult r
		where f.id = :id
		  and f.memberAccountId = :memberId
		  and f.coachAssignmentId is null
		  and f.trainingPlanVersionId is null
		  and f.trainingResultId = r.id
		  and r.classSessionId = :sessionId
		  and r.coachAssignmentId is null
		""")
	Optional<TrainingFeedback> findSessionByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	interface MemberFeedbackHistoryView {
		TrainingFeedback getFeedback();
		UUID getClassSessionId();
	}
}
