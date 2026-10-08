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
public class SportClassService {

	private final SportClassRepository classes;
	private final DisciplineRepository disciplines;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;

	SportClassService(SportClassRepository classes, DisciplineRepository disciplines, AccountRepository accounts,
			AuditEventRepository audits) {
		this.classes = classes;
		this.disciplines = disciplines;
		this.accounts = accounts;
		this.audits = audits;
	}

	@Transactional(readOnly = true)
	List<SportClassResponse> list(UUID managerId) {
		ensureActiveManager(managerId);
		Sort sort = Sort.by("name").ascending().and(Sort.by("id").ascending());
		return classes.findAll(sort).stream().map(SportClassResponse::from).toList();
	}

	@Transactional(readOnly = true)
	SportClassResponse get(UUID managerId, UUID classId) {
		ensureActiveManager(managerId);
		return SportClassResponse.from(findClass(classId));
	}

	@Transactional
	SportClassResponse create(UUID managerId, SportClassCreateRequest request) {
		ensureActiveManager(managerId);
		validateCreate(request);
		Discipline discipline = disciplines.findById(request.disciplineId())
			.orElseThrow(SportClassException::disciplineNotFound);
		if (discipline.getStatus() != DisciplineStatus.ACTIVE) {
			throw SportClassException.inactiveDiscipline();
		}
		String name = normalizeName(request.name());
		if (classes.existsByDisciplineIdAndName(discipline.getId(), name)) {
			throw SportClassException.duplicateName();
		}
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, name, request.classType(),
			normalizeDescription(request.description()), SportClassStatus.ACTIVE);
		flush(sportClass);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SPORT_CLASS_CREATED", "SPORT_CLASS",
			sportClass.getId(), snapshot(sportClass)));
		return SportClassResponse.from(sportClass);
	}

	@Transactional
	SportClassResponse update(UUID managerId, UUID classId, SportClassPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null || (request.name() == null && request.classType() == null
				&& request.description() == null && request.status() == null)) {
			throw SportClassException.validation("request", "at least one field is required");
		}
		SportClass sportClass = findClass(classId);
		Map<String, Object> before = snapshot(sportClass);
		String name = request.name() == null ? sportClass.getName() : normalizeName(request.name());
		if (!name.equals(sportClass.getName()) && classes.existsByDisciplineIdAndNameAndIdNot(
				sportClass.getDiscipline().getId(), name, classId)) {
			throw SportClassException.duplicateName();
		}
		SportClassType classType = request.classType() == null ? sportClass.getClassType() : request.classType();
		String description = request.description() == null
			? sportClass.getDescription() : normalizeDescription(request.description());
		SportClassStatus status = request.status() == null ? sportClass.getStatus() : request.status();
		sportClass.update(name, classType, description, status);
		Map<String, Object> after = snapshot(sportClass);
		if (before.equals(after)) return SportClassResponse.from(sportClass);
		flush(sportClass);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SPORT_CLASS_UPDATED", "SPORT_CLASS",
			classId, null, before, after));
		return SportClassResponse.from(sportClass);
	}

	private Map<String, Object> snapshot(SportClass sportClass) {
		Map<String, Object> data = new HashMap<>();
		data.put("disciplineId", sportClass.getDiscipline().getId().toString());
		data.put("name", sportClass.getName());
		data.put("classType", sportClass.getClassType().name());
		data.put("description", sportClass.getDescription());
		data.put("status", sportClass.getStatus().name());
		return data;
	}

	private void validateCreate(SportClassCreateRequest request) {
		if (request == null) throw SportClassException.validation("request", "must be a JSON object");
		if (request.disciplineId() == null) throw SportClassException.validation("disciplineId", "is required");
		if (request.classType() == null) throw SportClassException.validation("classType", "is required");
	}

	private SportClass findClass(UUID id) {
		return classes.findById(id).orElseThrow(SportClassException::notFound);
	}

	private void flush(SportClass sportClass) {
		try {
			classes.saveAndFlush(sportClass);
		}
		catch (DataIntegrityViolationException exception) {
			throw SportClassException.duplicateName();
		}
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String normalizeName(String value) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > 150) {
			throw SportClassException.validation("name", "must be non-blank and at most 150 characters");
		}
		return value.trim();
	}

	private String normalizeDescription(String value) {
		if (value == null || value.trim().isEmpty()) return null;
		return value.trim();
	}
}
