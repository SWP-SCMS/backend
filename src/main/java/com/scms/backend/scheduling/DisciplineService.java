package com.scms.backend.scheduling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.membership.MembershipBenefitRepository;
import com.scms.backend.membership.MembershipBenefitStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisciplineService {

	private final DisciplineRepository disciplines;
	private final SportClassRepository classes;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final MembershipBenefitRepository benefits;

	DisciplineService(DisciplineRepository disciplines, SportClassRepository classes, AccountRepository accounts,
			AuditEventRepository audits, MembershipBenefitRepository benefits) {
		this.disciplines = disciplines;
		this.classes = classes;
		this.accounts = accounts;
		this.audits = audits;
		this.benefits = benefits;
	}

	@Transactional(readOnly = true)
	List<DisciplineResponse> list(UUID managerId) {
		ensureActiveManager(managerId);
		return disciplines.findAllWithRequiredBenefitCodes().stream().map(DisciplineResponse::from).toList();
	}

	@Transactional(readOnly = true)
	DisciplineResponse get(UUID managerId, UUID disciplineId) {
		ensureActiveManager(managerId);
		return DisciplineResponse.from(find(disciplineId));
	}

	@Transactional
	DisciplineResponse create(UUID managerId, DisciplineCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) {
			throw new DisciplineValidationException("request", "must be a JSON object");
		}
		String name = normalizeName(request.name());
		if (isLegacyYoga(name)) {
			throw new DisciplineValidationException("name",
				"Yoga is retained for legacy history and cannot be created.");
		}
		if (disciplines.existsByName(name)) {
			throw new DuplicateDisciplineException();
		}
		Discipline discipline = new Discipline(UUID.randomUUID(), name, normalizeDescription(request.description()),
			DisciplineStatus.ACTIVE);
		discipline.replaceRequiredBenefitCodes(normalizeRequiredBenefitCodes(request.requiredBenefitCodes()));
		try {
			disciplines.saveAndFlush(discipline);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateDisciplineException();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "DISCIPLINE_CREATED", "DISCIPLINE",
			discipline.getId(), snapshot(discipline)));
		return DisciplineResponse.from(discipline);
	}

	@Transactional
	DisciplineResponse update(UUID managerId, UUID disciplineId, DisciplinePatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || (request.name() == null && request.description() == null && request.status() == null
				&& request.requiredBenefitCodes() == null)) {
			throw new DisciplineValidationException("request", "at least one field is required");
		}
		if (request.status() == DisciplineStatus.INACTIVE) {
			classes.findByDisciplineIdForUpdate(disciplineId);
		}
		Discipline discipline = findForUpdate(disciplineId);
		Map<String, Object> before = snapshot(discipline);
		String name = request.name() == null ? discipline.getName() : normalizeName(request.name());
		if (!isLegacyYoga(discipline.getName()) && isLegacyYoga(name)) {
			throw new DisciplineValidationException("name",
				"Yoga is retained for legacy history and cannot be assigned to a new discipline.");
		}
		if (!name.equals(discipline.getName()) && disciplines.existsByNameAndIdNot(name, disciplineId)) {
			throw new DuplicateDisciplineException();
		}
		String description = request.description() == null
			? discipline.getDescription() : normalizeDescription(request.description());
		DisciplineStatus status = request.status() == null ? discipline.getStatus() : request.status();
		if (request.requiredBenefitCodes() != null) {
			List<String> requiredBenefitCodes = normalizeRequiredBenefitCodes(request.requiredBenefitCodes());
			if (!discipline.getRequiredBenefitCodes().equals(requiredBenefitCodes)) {
				discipline.replaceRequiredBenefitCodes(requiredBenefitCodes);
			}
		}
		if (discipline.getStatus() != DisciplineStatus.INACTIVE && status == DisciplineStatus.INACTIVE) {
			List<DisciplineSessionConflict> blockers = disciplines.findBlockingSessions(disciplineId).stream()
				.map(DisciplineSessionConflict::from).toList();
			if (!blockers.isEmpty()) {
				throw new DisciplineValidationException("affectedSessionIds",
					"Discipline cannot be disabled while sessions are in progress or scheduled in the future: "
						+ blockers.stream().map(DisciplineSessionConflict::sessionId).toList(), blockers);
			}
		}
		discipline.update(name, description, status);
		Map<String, Object> after = snapshot(discipline);
		if (before.equals(after)) return DisciplineResponse.from(discipline);
		try {
			disciplines.saveAndFlush(discipline);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateDisciplineException();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "DISCIPLINE_UPDATED", "DISCIPLINE",
			disciplineId, null, before, after));
		return DisciplineResponse.from(discipline);
	}

	private Map<String, Object> snapshot(Discipline discipline) {
		Map<String, Object> data = new HashMap<>();
		data.put("name", discipline.getName());
		data.put("description", discipline.getDescription());
		data.put("status", discipline.getStatus().name());
		data.put("requiredBenefitCodes", discipline.getEffectiveRequiredBenefitCodes());
		return data;
	}

	private List<String> normalizeRequiredBenefitCodes(List<String> values) {
		if (values == null || values.isEmpty()) return List.of();
		List<String> normalized = new ArrayList<>(values.size());
		for (String value : values) {
			if (value == null || value.trim().isEmpty()) {
				throw invalidRequiredBenefits();
			}
			normalized.add(value.trim().toUpperCase(Locale.ROOT));
		}
		if (new HashSet<>(normalized).size() != normalized.size()) throw invalidRequiredBenefits();
		var resolved = benefits.findAllByCodeIn(normalized);
		if (resolved.size() != normalized.size() || resolved.stream()
				.anyMatch(benefit -> benefit.getStatus() != MembershipBenefitStatus.ACTIVE)) {
			throw invalidRequiredBenefits();
		}
		return normalized.stream().filter(code -> !"CLASS_BOOKING".equals(code)).sorted().toList();
	}

	private DisciplineValidationException invalidRequiredBenefits() {
		return new DisciplineValidationException("requiredBenefitCodes",
			"must contain unique, non-blank codes for ACTIVE Benefits");
	}

	private Discipline find(UUID id) {
		return disciplines.findById(id).orElseThrow(DisciplineNotFoundException::new);
	}

	private Discipline findForUpdate(UUID id) {
		java.util.Optional<Discipline> result = disciplines.findByIdForUpdate(id);
		if (result == null || result.isEmpty()) result = disciplines.findById(id);
		return result.orElseThrow(DisciplineNotFoundException::new);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String normalizeName(String value) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > 150) {
			throw new DisciplineValidationException("name", "must be non-blank and at most 150 characters");
		}
		return value.trim();
	}

	private String normalizeDescription(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}

	private boolean isLegacyYoga(String name) {
		return "yoga".equalsIgnoreCase(name.trim());
	}
}
