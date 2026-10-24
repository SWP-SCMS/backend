package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CoachLeaveIdentityIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CoachLeaveService service;
	@Autowired CoachLeaveRepository leaves;
	@Autowired AccountRepository accounts;

	@Test
	void existingLeaveReturnsItsCurrentCoachIdentityInManagerLists() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager", "0900000001");
		Account coach = saveAccount(AccountRole.COACH, "Nguyen Van Coach", "0900000002");
		CoachLeaveRequest leave = leaves.saveAndFlush(new CoachLeaveRequest(UUID.randomUUID(), coach,
			Instant.parse("2030-01-01T02:00:00Z"), Instant.parse("2030-01-01T03:00:00Z"), "Leave"));

		for (var result : java.util.List.of(service.managerList(manager.getId(), null),
			service.managerList(manager.getId(), CoachLeaveStatus.PENDING))) {
			assertThat(result).filteredOn(response -> response.id().equals(leave.getId()))
				.singleElement().satisfies(response -> {
					assertThat(response.coachId()).isEqualTo(coach.getId());
					assertThat(response.coachFullName()).isEqualTo("Nguyen Van Coach");
					assertThat(response.coachPhone()).isEqualTo("0900000002");
				});
		}
	}

	private Account saveAccount(AccountRole role, String name, String phone) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name, phone,
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
