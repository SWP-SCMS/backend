package com.scms.backend.membership;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipPlanAdminService {

	private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");
	private final MembershipPlanRepository plans;
	private final MembershipBenefitRepository benefits;
	private final MembershipOrderRepository orders;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;

	MembershipPlanAdminService(MembershipPlanRepository plans, MembershipBenefitRepository benefits,
			MembershipOrderRepository orders, AccountRepository accounts, AuditEventRepository audits) {
		this.plans = plans;
		this.benefits = benefits;
		this.orders = orders;
		this.accounts = accounts;
		this.audits = audits;
	}

	@Transactional(readOnly = true)
	List<MembershipBenefitResponse> catalog(UUID managerId) {
		ensureActiveManager(managerId);
		return benefits.findAllByOrderByCode().stream().map(MembershipBenefitResponse::from).toList();
	}

	@Transactional(readOnly = true)
	List<MembershipPlanResponse> list(UUID managerId) {
		ensureActiveManager(managerId);
		return plans.findAllWithBenefits().stream().map(MembershipPlanResponse::from).toList();
	}

	@Transactional(readOnly = true)
	MembershipPlanResponse get(UUID managerId, String code) {
		ensureActiveManager(managerId);
		return MembershipPlanResponse.from(find(code));
	}

	@Transactional
	MembershipPlanResponse create(UUID managerId, MembershipPlanCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw invalid("request is required");
		String code = normalizeCode(request.code());
		if (plans.existsById(code)) throw new MembershipPlanConflictException("MEMBERSHIP_PLAN_CODE_CONFLICT");
		Set<MembershipBenefit> resolved = resolveBenefits(request.benefitCodes());
		MembershipPlan plan = new MembershipPlan(code, normalizeDisplayName(request.displayName()),
			request.status() == null ? MembershipPlanStatus.ACTIVE : request.status(), resolved);
		try {
			plans.saveAndFlush(plan);
		}
		catch (DataIntegrityViolationException exception) {
			throw new MembershipPlanConflictException("MEMBERSHIP_PLAN_CODE_CONFLICT");
		}
		audits.save(new AuditEvent(UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8)), managerId,
			"MEMBERSHIP_PLAN_CREATED", "MEMBERSHIP_PLAN", auditId(code), snapshot(plan)));
		return MembershipPlanResponse.from(plan);
	}

	@Transactional
	MembershipPlanResponse update(UUID managerId, String code, MembershipPlanPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || (request.displayName() == null && request.status() == null
				&& request.benefitCodes() == null && request.code() == null)) throw invalid("at least one field is required");
		if (request.code() != null) throw invalid("code cannot be changed");
		MembershipPlan plan = find(code);
		Map<String, Object> before = snapshot(plan);
		Set<MembershipBenefit> resolved = plan.getBenefits();
		if (request.benefitCodes() != null) {
			Set<String> requestedCodes = normalizeBenefitCodes(request.benefitCodes());
			boolean locked = orders.existsByPlanCodeSnapshot(code);
			if (!locked || !codes(plan.getBenefits()).equals(requestedCodes)) {
				resolved = resolveBenefits(request.benefitCodes());
				if (locked && !codes(plan.getBenefits()).equals(codes(resolved))) {
					throw new MembershipPlanConflictException("MEMBERSHIP_PLAN_BENEFITS_LOCKED");
				}
			}
		}
		plan.update(request.displayName() == null ? plan.getDisplayName() : normalizeDisplayName(request.displayName()),
			request.status() == null ? plan.getStatus() : request.status(), resolved);
		Map<String, Object> after = snapshot(plan);
		if (!before.equals(after)) {
			plans.saveAndFlush(plan);
			audits.save(new AuditEvent(UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8)), managerId,
				"MEMBERSHIP_PLAN_UPDATED", "MEMBERSHIP_PLAN", auditId(code), null, before, after));
		}
		return MembershipPlanResponse.from(plan);
	}

	private MembershipPlan find(String code) {
		return plans.findByIdWithBenefits(code).orElseThrow(MembershipPlanNotFoundException::new);
	}

	private Set<MembershipBenefit> resolveBenefits(List<String> requestedCodes) {
		if (requestedCodes == null || requestedCodes.isEmpty()) throw invalid("benefitCodes must not be empty");
		Set<String> codes = normalizeBenefitCodes(requestedCodes);
		if (codes.size() != requestedCodes.size()) throw invalid("benefitCodes must be unique");
		List<MembershipBenefit> resolved = benefits.findAllByCodeIn(codes);
		if (resolved.size() != codes.size() || resolved.stream().anyMatch(benefit -> benefit.getStatus() != MembershipBenefitStatus.ACTIVE)) {
			throw invalid("benefitCodes must contain only existing ACTIVE Benefits");
		}
		return new HashSet<>(resolved);
	}

	private Set<String> normalizeBenefitCodes(List<String> requestedCodes) {
		if (requestedCodes == null || requestedCodes.isEmpty()) throw invalid("benefitCodes must not be empty");
		Set<String> codes = requestedCodes.stream().map(this::normalizeBenefitCode)
			.collect(java.util.stream.Collectors.toSet());
		if (codes.size() != requestedCodes.size()) throw invalid("benefitCodes must be unique");
		return codes;
	}

	private String normalizeCode(String value) {
		if (value == null || !CODE.matcher(value.trim().toUpperCase()).matches()) throw invalid("code is invalid");
		return value.trim().toUpperCase();
	}

	private String normalizeBenefitCode(String value) {
		if (value == null || value.trim().isEmpty()) throw invalid("benefitCodes contains an invalid code");
		return value.trim().toUpperCase();
	}

	private String normalizeDisplayName(String value) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > 100) throw invalid("displayName is invalid");
		return value.trim();
	}

	private MembershipPlanValidationException invalid(String message) { return new MembershipPlanValidationException(message); }

	private void ensureActiveManager(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private Map<String, Object> snapshot(MembershipPlan plan) {
		Map<String, Object> data = new HashMap<>();
		data.put("code", plan.getPlanCode());
		data.put("displayName", plan.getDisplayName());
		data.put("status", plan.getStatus().name());
		data.put("benefitCodes", codes(plan.getBenefits()));
		return data;
	}

	private Set<String> codes(Set<MembershipBenefit> values) {
		return values.stream().map(MembershipBenefit::getCode).collect(java.util.stream.Collectors.toSet());
	}

	private UUID auditId(String code) { return UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8)); }
}
