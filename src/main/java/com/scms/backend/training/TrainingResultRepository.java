package com.scms.backend.training;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.scheduling.SportClassType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingResultRepository extends JpaRepository<TrainingResult, UUID> {

	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.coachAssignmentId is not null
		  and r.classSessionId is null
		""")
	Optional<TrainingResult> findPersonalByIdAndMemberAccountId(@Param("id") UUID id,
			@Param("memberId") UUID memberId);

	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.classSessionId = :sessionId
		  and r.coachAssignmentId is null
		""")
	Optional<TrainingResult> findSessionByIdAndMemberAccountId(@Param("id") UUID id,
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	@Query(value = """
		select * from training_results
		where class_session_id = :sessionId
		  and member_account_id = :memberId
		  and coach_assignment_id is null
		  and legacy_duplicate = false
		""", nativeQuery = true)
	Optional<TrainingResult> findSessionByClassSessionIdAndMemberAccountId(
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	@Query("""
		select r as result, coach.fullName as coachName, sportClass.name as sessionName,
		  session.startTime as sessionStartTime, sportClass.classType as classType
		from TrainingResult r
		join Account coach on coach.id = r.createdByAccountId
		left join ClassSession session on session.id = r.classSessionId
		left join session.sportClass sportClass
		where r.memberAccountId = :memberId
		order by r.activityAt desc, r.id desc
		""")
	List<MemberTrainingResultHistoryView> findHistoryByMemberAccountId(@Param("memberId") UUID memberId);

	@Query(value = """
		select r as result, coach.fullName as coachName, sportClass.name as sessionName,
		  session.startTime as sessionStartTime, sportClass.classType as classType
		from TrainingResult r
		join Account coach on coach.id = r.createdByAccountId
		left join ClassSession session on session.id = r.classSessionId
		left join session.sportClass sportClass
		where r.memberAccountId = :memberId
		order by r.activityAt desc, r.id desc
		""", countQuery = "select count(r) from TrainingResult r where r.memberAccountId = :memberId")
	Page<MemberTrainingResultHistoryView> findHistoryByMemberAccountId(@Param("memberId") UUID memberId,
			org.springframework.data.domain.Pageable pageable);

	@Query(value = """
		select * from training_results
		where class_session_id = :sessionId
		  and member_account_id = :memberId
		  and coach_assignment_id is null
		  and legacy_duplicate = false
		for update
		""", nativeQuery = true)
	Optional<TrainingResult> findSessionByClassSessionIdAndMemberAccountIdForUpdate(
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.coachAssignmentId is not null
		  and r.classSessionId is null
		""")
	Optional<TrainingResult> findPersonalByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.classSessionId = :sessionId
		  and r.coachAssignmentId is null
		""")
	Optional<TrainingResult> findSessionByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

	interface MemberTrainingResultHistoryView {
		TrainingResult getResult();
		String getCoachName();
		String getSessionName();
		Instant getSessionStartTime();
		SportClassType getClassType();
	}
}
