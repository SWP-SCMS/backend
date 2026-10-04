package com.scms.backend.manager;

import java.time.Clock;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffStatusService {

	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final JdbcTemplate jdbc;
	private final Clock clock;

	StaffStatusService(AccountRepository accounts, AuditEventRepository audits, JdbcTemplate jdbc, Clock clock) {
		this.accounts = accounts;
		this.audits = audits;
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Transactional
	StaffAccountResponse deactivate(UUID managerId, UUID accountId, StaffStatusChangeRequest request) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		validate(request);
		Account target = accounts.findById(accountId)
			.filter(account -> account.getRole() != AccountRole.MEMBER)
			.orElseThrow(StaffAccountNotFoundException::new);
		if (managerId.equals(accountId)) {
			throw new StaffStatusConflictException("A Manager cannot deactivate their own account");
		}
		if (target.getStatus() == AccountStatus.INACTIVE) {
			throw new StaffStatusConflictException("Staff account is already INACTIVE");
		}
		if (target.getRole() == AccountRole.MANAGER && accounts.findActiveManagersForUpdate().size() <= 1) {
			throw new StaffStatusConflictException("The last active Manager cannot be deactivated");
		}
		if (target.getRole() == AccountRole.COACH) {
			ensureCoachHasNoOutstandingWork(accountId);
		}

		AccountStatus before = target.getStatus();
		target.deactivateStaff();
		accounts.flush();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "STAFF_DEACTIVATED", "ACCOUNT", accountId,
			request.reason().trim(), Map.of("status", before.name()), Map.of("status", AccountStatus.INACTIVE.name())));
		return new StaffAccountResponse(target.getId(), target.getRole(), target.getStatus(), target.getFullName(),
			target.getPhone(), target.getEmail(), target.getBirthDate());
	}

	private void ensureCoachHasNoOutstandingWork(UUID coachId) {
		Boolean hasFutureSessions = jdbc.queryForObject("""
			select exists(select 1 from class_sessions where teaching_coach_account_id = ?
				and start_time > ? and status in ('SCHEDULED', 'IN_PROGRESS'))
			""", Boolean.class, coachId, Timestamp.from(clock.instant()));
		Boolean hasActiveAssignments = jdbc.queryForObject("""
			select exists(select 1 from coach_assignments where coach_account_id = ? and status = 'ACTIVE')
			""", Boolean.class, coachId);
		if (Boolean.TRUE.equals(hasFutureSessions) || Boolean.TRUE.equals(hasActiveAssignments)) {
			throw new StaffStatusConflictException("Coach still has future sessions or active personal assignments");
		}
	}

	private void validate(StaffStatusChangeRequest request) {
		if (request == null || request.status() != AccountStatus.INACTIVE) {
			throw new StaffAccountValidationException("status", "must be INACTIVE");
		}
		if (request.reason() == null || request.reason().trim().isEmpty() || request.reason().trim().length() > 1000) {
			throw new StaffAccountValidationException("reason", "must be non-blank and at most 1000 characters");
		}
	}
}
