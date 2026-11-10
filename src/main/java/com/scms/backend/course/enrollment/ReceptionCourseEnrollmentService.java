package com.scms.backend.course.enrollment;

import static com.scms.backend.course.enrollment.CourseEnrollmentApiModels.ReceptionCourseEnrollment;
import static com.scms.backend.course.enrollment.CourseEnrollmentApiModels.ReceptionCourseEnrollmentPage;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReceptionCourseEnrollmentService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final Clock clock;

	ReceptionCourseEnrollmentService(JdbcTemplate db, AccountRepository accounts, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.clock = clock;
		this.receptionCourseEnrollmentRepository = new ReceptionCourseEnrollmentRepository(this.db, this.clock);
	}

	@Transactional(readOnly = true)
	ReceptionCourseEnrollmentPage list(UUID receptionistId, UUID memberId, int page, int size, String status, Boolean eligible) {
		active(receptionistId);
		page(page, size);
		String normalizedStatus = status(status);
		return enrollmentPage(memberId, page, size, eligible, normalizedStatus);
	}

	@Transactional(readOnly = true)
	ReceptionCourseEnrollment detail(UUID receptionistId, UUID memberId, UUID enrollmentId) {
		active(receptionistId);
		return enrollmentDetail(memberId, enrollmentId);
	}

	private BigInteger amount(BigDecimal value) {
		return receptionCourseEnrollmentRepository.amount(value);
	}

	private String status(String value) {
		if (value == null || value.isBlank())
			return null;
		String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
		if (!Set.of("ACTIVE", "TRANSFERRED", "COMPLETED", "WITHDRAWAL_PENDING", "WITHDRAWN").contains(normalized))
			throw CourseException.validation("status", "Unknown enrollment status.");
		return normalized;
	}

	private void active(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private void page(int page, int size) {
		if (page < 0 || size < 1 || size > 100)
			throw CourseException.validation("page", "Invalid page or size.");
	}

	private ReceptionCourseEnrollmentPage enrollmentPage(UUID memberId, int page, int size, Boolean eligible, String normalizedStatus) {
		return receptionCourseEnrollmentRepository.enrollmentPage(memberId, page, size, eligible, normalizedStatus);
	}

	private ReceptionCourseEnrollment enrollmentDetail(UUID memberId, UUID enrollmentId) {
		return receptionCourseEnrollmentRepository.enrollmentDetail(memberId, enrollmentId);
	}

	private final ReceptionCourseEnrollmentRepository receptionCourseEnrollmentRepository;
}
