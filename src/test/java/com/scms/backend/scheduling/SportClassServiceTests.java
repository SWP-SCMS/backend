package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SportClassServiceTests {

	@Mock SportClassRepository classes;
	@Mock DisciplineRepository disciplines;
	@Mock AccountRepository accounts;
	@InjectMocks SportClassService service;

	@Test
	void activeManagerCreatesNormalizedClassInActiveDiscipline() {
		UUID managerId = activeManager();
		Discipline discipline = discipline(DisciplineStatus.ACTIVE);
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));
		when(classes.existsByDisciplineIdAndName(discipline.getId(), "Beginner Yoga")).thenReturn(false);
		when(classes.saveAndFlush(any(SportClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SportClassResponse result = service.create(managerId, new SportClassCreateRequest(
			discipline.getId(), "  Beginner Yoga  ", SportClassType.YOGA, "  Foundation class  "));

		assertThat(result.disciplineId()).isEqualTo(discipline.getId());
		assertThat(result.name()).isEqualTo("Beginner Yoga");
		assertThat(result.classType()).isEqualTo(SportClassType.YOGA);
		assertThat(result.description()).isEqualTo("Foundation class");
		assertThat(result.status()).isEqualTo(SportClassStatus.ACTIVE);
	}

	@Test
	void createRejectsInactiveDiscipline() {
		UUID managerId = activeManager();
		Discipline discipline = discipline(DisciplineStatus.INACTIVE);
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));

		assertThatThrownBy(() -> service.create(managerId, new SportClassCreateRequest(
			discipline.getId(), "Yoga", SportClassType.YOGA, null)))
			.isInstanceOf(SportClassException.class)
			.extracting("code").isEqualTo("DISCIPLINE_INACTIVE");
	}

	@Test
	void createRejectsDuplicateNameWithinDiscipline() {
		UUID managerId = activeManager();
		Discipline discipline = discipline(DisciplineStatus.ACTIVE);
		when(disciplines.findById(discipline.getId())).thenReturn(Optional.of(discipline));
		when(classes.existsByDisciplineIdAndName(discipline.getId(), "Yoga")).thenReturn(true);

		assertThatThrownBy(() -> service.create(managerId, new SportClassCreateRequest(
			discipline.getId(), "Yoga", SportClassType.YOGA, null)))
			.isInstanceOf(SportClassException.class)
			.extracting("code").isEqualTo("CLASS_NAME_CONFLICT");
	}

	@Test
	void createRejectsMissingClassType() {
		UUID managerId = activeManager();
		Discipline discipline = discipline(DisciplineStatus.ACTIVE);

		assertThatThrownBy(() -> service.create(managerId,
			new SportClassCreateRequest(discipline.getId(), "Yoga", null, null)))
			.isInstanceOf(SportClassException.class)
			.extracting("field").isEqualTo("classType");
	}

	@Test
	void inactiveManagerCannotListClasses() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.list(managerId))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	@Test
	void listUsesStableNameThenIdOrdering() {
		UUID managerId = activeManager();
		SportClass sportClass = sportClass(discipline(DisciplineStatus.ACTIVE));
		when(classes.findAll(any(Sort.class))).thenReturn(List.of(sportClass));

		assertThat(service.list(managerId)).singleElement().extracting(SportClassResponse::name)
			.isEqualTo("Yoga");
		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(classes).findAll(sort.capture());
		assertThat(sort.getValue().getOrderFor("name")).isNotNull();
		assertThat(sort.getValue().getOrderFor("id")).isNotNull();
	}

	@Test
	void getReturnsNotFoundForUnknownClass() {
		UUID managerId = activeManager();
		UUID classId = UUID.randomUUID();
		when(classes.findById(classId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(managerId, classId))
			.isInstanceOf(SportClassException.class)
			.extracting("code").isEqualTo("CLASS_NOT_FOUND");
	}

	@Test
	void patchUpdatesProvidedFieldsAndCanDeactivate() {
		UUID managerId = activeManager();
		Discipline discipline = discipline(DisciplineStatus.ACTIVE);
		SportClass sportClass = sportClass(discipline);
		when(classes.findById(sportClass.getId())).thenReturn(Optional.of(sportClass));
		when(classes.existsByDisciplineIdAndNameAndIdNot(discipline.getId(), "Power Yoga", sportClass.getId()))
			.thenReturn(false);

		SportClassResponse result = service.update(managerId, sportClass.getId(),
			new SportClassPatchRequest("  Power Yoga  ", SportClassType.GROUP,
				"  Intermediate  ", SportClassStatus.INACTIVE));

		assertThat(result.name()).isEqualTo("Power Yoga");
		assertThat(result.classType()).isEqualTo(SportClassType.GROUP);
		assertThat(result.description()).isEqualTo("Intermediate");
		assertThat(result.status()).isEqualTo(SportClassStatus.INACTIVE);
	}

	@Test
	void patchRejectsEmptyBody() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.update(managerId, UUID.randomUUID(),
			new SportClassPatchRequest(null, null, null, null)))
			.isInstanceOf(SportClassException.class)
			.extracting("field").isEqualTo("request");
	}

	private UUID activeManager() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		return managerId;
	}

	private Discipline discipline(DisciplineStatus status) {
		return new Discipline(UUID.randomUUID(), "Mind Body", null, status);
	}

	private SportClass sportClass(Discipline discipline) {
		return new SportClass(UUID.randomUUID(), discipline, "Yoga", SportClassType.YOGA,
			"Foundation", SportClassStatus.ACTIVE);
	}
}
