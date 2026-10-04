package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private StaffAccountService service;

	@Test
	void activeManagerGetsOnlyStaffAccountsAndSafeResponse() {
		UUID managerId = UUID.randomUUID();
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE, "Coach One");
		when(accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(coach), PageRequest.of(0, 20), 1));

		StaffAccountPageResponse result = service.search(managerId, " coach ", AccountRole.COACH,
			AccountStatus.ACTIVE, PageRequest.of(0, 20));

		assertThat(result.content()).singleElement().extracting(StaffAccountResponse::fullName)
			.isEqualTo("Coach One");
		verify(accountRepository).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void memberRoleIsRejectedFromStaffSearch() {
		UUID managerId = UUID.randomUUID();
		when(accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);

		assertThatThrownBy(() -> service.search(managerId, null, AccountRole.MEMBER, null, PageRequest.of(0, 20)))
			.isInstanceOf(StaffAccountValidationException.class);
	}

	@Test
	void nonActiveManagerCannotSearchStaffAccounts() {
		UUID managerId = UUID.randomUUID();
		when(accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.search(managerId, null, null, null, PageRequest.of(0, 20)))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	private Account account(AccountRole role, AccountStatus status, String name) {
		return new Account(UUID.randomUUID(), role, status, name, "0900000000", name.replace(' ', '.') + "@test.local",
			LocalDate.of(1990, 1, 1), "hash");
	}
}
