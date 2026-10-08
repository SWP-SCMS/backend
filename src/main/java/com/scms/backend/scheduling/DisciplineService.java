package com.scms.backend.scheduling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisciplineService {

	private final DisciplineRepository disciplines;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;

	DisciplineService(DisciplineRepository disciplines, AccountRepository accounts, AuditEventRepository audits) {
		this.disciplines = disciplines;
		this.accounts = accounts;
		this.audits = audits;
	}

	@Transactional(readOnly = true)
	List<DisciplineResponse> list(UUID managerId) {
		ensureActiveManager(managerId);
		Sort sort = Sort.by("name").ascending().and(Sort.by("id").ascending());
		return disciplines.findAll(sort).stream().map(DisciplineResponse::from).toList();
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
		if (disciplines.existsByName(name)) {
			throw new DuplicateDisciplineException();
		}
		Discipline discipline = new Discipline(UUID.randomUUID(), name, normalizeDescription(request.description()),
			DisciplineStatus.ACTIVE);
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
		if (request == null || (request.name() == null && request.description() == null && request.status() == null)) {
			throw new DisciplineValidationException("request", "at least one field is required");
		}
		Discipline discipline = find(disciplineId);
		Map<String, Object> before = snapshot(discipline);
		String name = request.name() == null ? discipline.getName() : normalizeName(request.name());
		if (!name.equals(discipline.getName()) && disciplines.existsByNameAndIdNot(name, disciplineId)) {
			throw new DuplicateDisciplineException();
		}
		String description = request.description() == null
			? discipline.getDescription() : normalizeDescription(request.description());
		DisciplineStatus status = request.status() == null ? discipline.getStatus() : request.status();
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
		return data;
	}

	private Discipline find(UUID id) {
		return disciplines.findById(id).orElseThrow(DisciplineNotFoundException::new);
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
}
