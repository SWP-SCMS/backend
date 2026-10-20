package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.membership.MembershipBenefit;
import com.scms.backend.membership.MembershipBenefitRepository;
import com.scms.backend.membership.MembershipBenefitStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisciplineServiceTests {

	@Mock
	private DisciplineRepository disciplines;

	@Mock
	private SportClassRepository classes;

	@Mock
	private AccountRepository accounts;

	@Mock
	private AuditEventRepository audits;

	@Mock
	private MembershipBenefitRepository benefits;

	@InjectMocks
	private DisciplineService service;

	@Test
	void activeManagerRejectsLegacyYogaDisciplineCreation() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.create(managerId,
			new DisciplineCreateRequest("  Yoga  ", "  Mobility and balance  ")))
			.isInstanceOf(DisciplineValidationException.class)
			.hasMessage("Yoga is retained for legacy history and cannot be created.");
		verify(disciplines, never()).saveAndFlush(any(Discipline.class));
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
		when(disciplines.existsByName("Pilates")).thenReturn(false);

		DisciplineResponse response = service.create(managerId,
			new DisciplineCreateRequest("Pilates", null, null));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(response.requiredBenefitCodes()).containsExactly("CLASS_BOOKING");
		assertThat(audit.getValue().getAfterData()).containsEntry("description", null)
			.containsEntry("requiredBenefitCodes", List.of("CLASS_BOOKING"));
	}

	@Test
	void createNormalizesSortsAndStoresOnlySpecializedBenefits() {
		UUID managerId = activeManager();
		when(disciplines.existsByName("Pilates")).thenReturn(false);
		MembershipBenefit poolAccess = activeBenefit("POOL_ACCESS");
		MembershipBenefit classBooking = activeBenefit("CLASS_BOOKING");
		when(benefits.findAllByCodeIn(List.of("POOL_ACCESS", "CLASS_BOOKING")))
			.thenReturn(List.of(poolAccess, classBooking));

		DisciplineResponse response = service.create(managerId,
			new DisciplineCreateRequest("Pilates", null, List.of(" pool_access ", "class_booking")));

		assertThat(response.requiredBenefitCodes()).containsExactly("CLASS_BOOKING", "POOL_ACCESS");
		ArgumentCaptor<Discipline> saved = ArgumentCaptor.forClass(Discipline.class);
		verify(disciplines).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getRequiredBenefitCodes()).containsExactly("POOL_ACCESS");
	}

	@Test
	void createRejectsNullBlankAndDuplicateBenefitCodes() {
		UUID managerId = activeManager();

		for (List<String> codes : List.of(java.util.Arrays.asList("POOL_ACCESS", null),
			List.of("POOL_ACCESS", "  "), List.of("pool_access", " POOL_ACCESS "))) {
			assertThatThrownBy(() -> service.create(managerId,
				new DisciplineCreateRequest("Pilates", null, codes)))
				.isInstanceOf(DisciplineValidationException.class)
				.extracting("field").isEqualTo("requiredBenefitCodes");
		}
	}

	@Test
	void createRejectsUnknownOrInactiveBenefitCodes() {
		UUID managerId = activeManager();
		when(benefits.findAllByCodeIn(List.of("UNKNOWN"))).thenReturn(List.of());
		MembershipBenefit inactivePoolAccess = benefit("POOL_ACCESS", MembershipBenefitStatus.INACTIVE);
		when(benefits.findAllByCodeIn(List.of("POOL_ACCESS")))
			.thenReturn(List.of(inactivePoolAccess));

		for (List<String> codes : List.of(List.of("UNKNOWN"), List.of("pool_access"))) {
			assertThatThrownBy(() -> service.create(managerId,
				new DisciplineCreateRequest("Pilates", null, codes)))
				.isInstanceOf(DisciplineValidationException.class)
				.extracting("field").isEqualTo("requiredBenefitCodes");
		}
	}

	@Test
	void createRejectsDuplicateNormalizedName() {
		UUID managerId = activeManager();
		when(disciplines.existsByName("Pilates")).thenReturn(true);

		assertThatThrownBy(() -> service.create(managerId, new DisciplineCreateRequest("Pilates", null)))
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
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		discipline.replaceRequiredBenefitCodes(List.of("POOL_ACCESS", "MASSAGE_ACCESS"));
		when(disciplines.findAllWithRequiredBenefitCodes()).thenReturn(List.of(discipline));

		assertThat(service.list(managerId)).singleElement().satisfies(response -> {
			assertThat(response.name()).isEqualTo("Yoga");
			assertThat(response.requiredBenefitCodes())
				.containsExactly("CLASS_BOOKING", "MASSAGE_ACCESS", "POOL_ACCESS");
		});
		verify(disciplines).findAllWithRequiredBenefitCodes();
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
			"name", "Yoga", "description", "Old", "status", "ACTIVE",
			"requiredBenefitCodes", List.of("CLASS_BOOKING")));
		assertThat(audit.getValue().getAfterData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"name", "Pilates", "description", "Core control", "status", "INACTIVE",
			"requiredBenefitCodes", List.of("CLASS_BOOKING")));
	}

	@Test
	void patchNullBenefitsPreservesExistingRequirements() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Pilates", null, DisciplineStatus.ACTIVE);
		discipline.replaceRequiredBenefitCodes(List.of("POOL_ACCESS"));
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));

		DisciplineResponse result = service.update(managerId, discipline.getId(),
			new DisciplinePatchRequest("Pilates 2", null, null, null));

		assertThat(result.requiredBenefitCodes()).containsExactly("CLASS_BOOKING", "POOL_ACCESS");
		assertThat(discipline.getRequiredBenefitCodes()).containsExactly("POOL_ACCESS");
	}

	@Test
	void patchEmptyBenefitsRemovesSpecializedRequirements() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Pilates", null, DisciplineStatus.ACTIVE);
		discipline.replaceRequiredBenefitCodes(List.of("POOL_ACCESS"));
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));

		DisciplineResponse result = service.update(managerId, discipline.getId(),
			new DisciplinePatchRequest(null, null, null, List.of()));

		assertThat(result.requiredBenefitCodes()).containsExactly("CLASS_BOOKING");
		assertThat(discipline.getRequiredBenefitCodes()).isEmpty();
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getBeforeData())
			.containsEntry("requiredBenefitCodes", List.of("CLASS_BOOKING", "POOL_ACCESS"));
		assertThat(audit.getValue().getAfterData())
			.containsEntry("requiredBenefitCodes", List.of("CLASS_BOOKING"));
	}

	@Test
	void disableLocksClassesBeforeDisciplineAndReturnsStableAffectedDetails() {
		UUID managerId = activeManager();
		UUID disciplineId = UUID.randomUUID();
		Discipline discipline = new Discipline(disciplineId, "Yoga", null, DisciplineStatus.ACTIVE);
		UUID firstSessionId = UUID.randomUUID();
		UUID firstClassId = UUID.randomUUID();
		UUID secondSessionId = UUID.randomUUID();
		UUID secondClassId = UUID.randomUUID();
		DisciplineRepository.BlockingSessionView first = mock(DisciplineRepository.BlockingSessionView.class);
		DisciplineRepository.BlockingSessionView second = mock(DisciplineRepository.BlockingSessionView.class);
		when(disciplines.findByIdForUpdate(disciplineId)).thenReturn(Optional.of(discipline));
		when(disciplines.findBlockingSessions(disciplineId)).thenReturn(List.of(first, second));
		when(first.getSessionId()).thenReturn(firstSessionId);
		when(first.getClassId()).thenReturn(firstClassId);
		when(first.getClassName()).thenReturn("Morning Yoga");
		when(second.getSessionId()).thenReturn(secondSessionId);
		when(second.getClassId()).thenReturn(secondClassId);
		when(second.getClassName()).thenReturn("Evening Yoga");

		DisciplineValidationException exception;
		try {
			service.update(managerId, disciplineId,
				new DisciplinePatchRequest(null, null, DisciplineStatus.INACTIVE));
			throw new AssertionError("Discipline disable should be rejected");
		}
		catch (DisciplineValidationException expected) {
			exception = expected;
		}

		assertThat(exception.getAffectedSessions()).extracting(DisciplineSessionConflict::sessionId)
			.containsExactly(firstSessionId, secondSessionId);
		assertThat(exception.getAffectedSessions()).extracting(DisciplineSessionConflict::classId)
			.containsExactly(firstClassId, secondClassId);
		assertThat(exception.getAffectedSessions()).extracting(DisciplineSessionConflict::className)
			.containsExactly("Morning Yoga", "Evening Yoga");
		InOrder order = org.mockito.Mockito.inOrder(classes, disciplines);
		order.verify(classes).findByDisciplineIdForUpdate(disciplineId);
		order.verify(disciplines).findByIdForUpdate(disciplineId);
		verify(disciplines, never()).saveAndFlush(any(Discipline.class));
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
	void patchRejectsChangingDisciplineToLegacyYoga() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Pilates", "Mobility", DisciplineStatus.ACTIVE);
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));

		assertThatThrownBy(() -> service.update(managerId, discipline.getId(),
			new DisciplinePatchRequest("Yoga", null, null)))
			.isInstanceOf(DisciplineValidationException.class)
			.hasMessage("Yoga is retained for legacy history and cannot be assigned to a new discipline.");
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

	private MembershipBenefit activeBenefit(String code) {
		return benefit(code, MembershipBenefitStatus.ACTIVE);
	}

	private MembershipBenefit benefit(String code, MembershipBenefitStatus status) {
		MembershipBenefit benefit = mock(MembershipBenefit.class);
		when(benefit.getStatus()).thenReturn(status);
		return benefit;
	}
}
