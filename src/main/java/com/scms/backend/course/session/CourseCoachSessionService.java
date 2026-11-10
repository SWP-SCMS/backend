package com.scms.backend.course.session;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.session.CourseCoachSessionApiModels.CourseCoachSessionPage;
import com.scms.backend.course.session.CourseCoachSessionApiModels.CourseCoachSessionResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseCoachSessionService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final Clock clock;

	CourseCoachSessionService(JdbcTemplate db, AccountRepository accounts, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.clock = clock;
		this.courseCoachSessionRepository = new CourseCoachSessionRepository(this.db, this.clock);
	}

	@Transactional(readOnly = true)
	CourseCoachSessionPage list(UUID coachId, Instant from, Instant to, String status, int page, int size, List<String> sort) {
		validate(coachId, from, to, status, page, size);
		return sessionPage(coachId, from, to, status, page, size, sort);
	}

	@Transactional(readOnly = true)
	public CourseCoachSessionResponse detail(UUID coachId, UUID sessionId) {
		active(coachId);
		return sessionDetail(coachId, sessionId);
	}

	private Query query(Instant from, Instant to, String status) {
		return courseCoachSessionRepository.query(from, to, status);
	}

	private void validate(UUID coachId, Instant from, Instant to, String status, int page, int size) {
		active(coachId);
		if (from != null && to != null && !from.isBefore(to))
			throw CourseException.validation("from", "must be before to");
		if (status != null)
			try {
				CourseSessionStatus.valueOf(status);
			} catch (IllegalArgumentException exception) {
				throw CourseException.validation("status", "must be a valid CourseSession status");
			}
		if (page < 0)
			throw CourseException.validation("page", "must be greater than or equal to zero");
		if (size < 1 || size > 100)
			throw CourseException.validation("size", "must be between 1 and 100");
	}

	private void active(UUID coachId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	record Query(String where, List<Object> args) {
	}

	private CourseCoachSessionPage sessionPage(UUID coachId, Instant from, Instant to, String status, int page, int size, List<String> sort) {
		return courseCoachSessionRepository.sessionPage(coachId, from, to, status, page, size, sort);
	}

	private CourseCoachSessionResponse sessionDetail(UUID coachId, UUID sessionId) {
		return courseCoachSessionRepository.sessionDetail(coachId, sessionId);
	}

	private final CourseCoachSessionRepository courseCoachSessionRepository;
}
