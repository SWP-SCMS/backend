package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DisciplineServiceTests {

	@Mock
	private DisciplineRepository disciplines;

	@Mock
	private AccountRepository accounts;

	@Mock
	private AuditEventRepository audits;

	@InjectMocks
	private DisciplineService service;

	@Test
	void activeManagerCreatesNormalizedActiveDiscipline() {
		UUID managerId = activeManager();
		when(disciplines.existsByName("Yoga")).thenReturn(false);
		when(disciplines.saveAndFlush(any(Discipline.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DisciplineResponse result = service.create(managerId,
			new DisciplineCreateRequest("  Yoga  ", "  Mobility and balance  "));

		assertThat(result.name()).isEqualTo("Yoga");
		assertThat(result.description()).isEqualTo("Mobility and balance");
		assertThat(result.status()).isEqualTo(DisciplineStatus.ACTIVE);
		ArgumentCaptor<Discipline> saved = ArgumentCaptor.forClass(Discipline.class);
		verify(disciplines).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getId()).isNotNull();
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits, times(1)).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("DISCIPLINE_CREATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getTargetType()).isEqualTo("DISCIPLINE");
		assertThat(audit.getValue().getTargetId()).isEqualTo(result.id());
		assertThat(audit.getValue().getBeforeData()).isNull();
		assertThat(audit.getValue().getAfterData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"name", "Yoga", "description", "Mobility and balance", "status", "ACTIVE"));
	}

	@Test
	void createRejectsBlankName() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.create(managerId, new DisciplineCreateRequest("   ", null)))
			.isInstanceOf(DisciplineValidationException.class)
			.hasMessage("must be non-blank and at most 150 characters");
	}

	@Test
	void createAuditIncludesNullDescription() {
		UUID managerId = activeManager();
		when(disciplines.existsByName("Yoga")).thenReturn(false);

		service.create(managerId, new DisciplineCreateRequest("Yoga", null));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAfterData()).containsEntry("description", null);
	}

	@Test
	void createRejectsDuplicateNormalizedName() {
		UUID managerId = activeManager();
		when(disciplines.existsByName("Yoga")).thenReturn(true);

		assertThatThrownBy(() -> service.create(managerId, new DisciplineCreateRequest("Yoga", null)))
			.isInstanceOf(DuplicateDisciplineException.class);
	}

	@Test
	void inactiveManagerCannotListDisciplines() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.list(managerId))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	@Test
	void listIsStableByNameThenId() {
		UUID managerId = activeManager();
		when(disciplines.findAll(any(Sort.class))).thenReturn(List.of(
			new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE)));

		assertThat(service.list(managerId)).singleElement().extracting(DisciplineResponse::name)
			.isEqualTo("Yoga");
		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(disciplines).findAll(sort.capture());
		assertThat(sort.getValue().getOrderFor("name")).isNotNull();
		assertThat(sort.getValue().getOrderFor("id")).isNotNull();
	}

	@Test
	void getReturnsNotFoundForUnknownDiscipline() {
		UUID managerId = activeManager();
		UUID disciplineId = UUID.randomUUID();
		when(disciplines.findById(disciplineId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(managerId, disciplineId))
			.isInstanceOf(DisciplineNotFoundException.class);
	}

	@Test
	void patchUpdatesProvidedFieldsAndCanDeactivate() {
		UUID managerId = activeManager();
		UUID disciplineId = UUID.randomUUID();
		Discipline discipline = new Discipline(disciplineId, "Yoga", "Old", DisciplineStatus.ACTIVE);
		when(disciplines.findById(disciplineId)).thenReturn(Optional.of(discipline));
		when(disciplines.existsByNameAndIdNot("Pilates", disciplineId)).thenReturn(false);

		DisciplineResponse result = service.update(managerId, disciplineId,
			new DisciplinePatchRequest("  Pilates ", "  Core control ", DisciplineStatus.INACTIVE));

		assertThat(result.name()).isEqualTo("Pilates");
		assertThat(result.description()).isEqualTo("Core control");
		assertThat(result.status()).isEqualTo(DisciplineStatus.INACTIVE);
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits, times(1)).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("DISCIPLINE_UPDATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getTargetType()).isEqualTo("DISCIPLINE");
		assertThat(audit.getValue().getTargetId()).isEqualTo(disciplineId);
		assertThat(audit.getValue().getBeforeData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"name", "Yoga", "description", "Old", "status", "ACTIVE"));
		assertThat(audit.getValue().getAfterData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"name", "Pilates", "description", "Core control", "status", "INACTIVE"));
	}

	@Test
	void normalizedNoOpPatchWritesNoAudit() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", "Mobility", DisciplineStatus.ACTIVE);
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));

		DisciplineResponse result = service.update(managerId, discipline.getId(),
			new DisciplinePatchRequest("  Yoga  ", " Mobility ", DisciplineStatus.ACTIVE));

		assertThat(result).isEqualTo(DisciplineResponse.from(discipline));
		verify(audits, never()).save(any());
	}

	@Test
	void patchRejectsEmptyBody() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.update(managerId, UUID.randomUUID(),
			new DisciplinePatchRequest(null, null, null)))
			.isInstanceOf(DisciplineValidationException.class)
			.hasMessage("at least one field is required");
	}

	private UUID activeManager() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		return managerId;
	}
}
