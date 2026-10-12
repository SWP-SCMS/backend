package com.scms.backend.audit;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditHistoryService {

	private final AuditEventRepository audits;
	private final AccountRepository accounts;

	AuditHistoryService(AuditEventRepository audits, AccountRepository accounts) {
		this.audits = audits;
		this.accounts = accounts;
	}

	@Transactional(readOnly = true)
	AuditHistoryPageResponse list(UUID managerId, UUID actorId, String action, String targetType, UUID targetId,
			Instant from, Instant to, Pageable pageable) {
		ensureActiveManager(managerId);
		if (from != null && to != null && !from.isBefore(to)) {
			throw new AuditHistoryValidationException("to", "must be after from");
		}
		String normalizedAction = normalize(action, "action", 120);
		String normalizedTargetType = normalize(targetType, "targetType", 80);
		Page<AuditEvent> page = audits.findAll(
			AuditHistorySpecifications.filtered(actorId, normalizedAction, normalizedTargetType, targetId, from, to),
			pageable);
		return new AuditHistoryPageResponse(page.getContent().stream().map(AuditHistoryResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String normalize(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (normalized.length() > maxLength) {
			throw new AuditHistoryValidationException(field, "must be at most " + maxLength + " characters");
		}
		return normalized;
	}
}
