package com.scms.backend.scheduling;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoachTeachingScheduleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mvc;
	@Autowired AccountRepository accounts;
	@Autowired ClassSessionRepository sessions;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate jdbc;

	@Test
	@Transactional
	void coachCanOnlyListAndReadAssignedSessions() throws Exception {
		Fixture fixture = fixture();

		mvc.perform(get("/coach/class-sessions")
				.param("from", "2029-12-31T00:00:00Z")
				.param("to", "2030-01-02T00:00:00Z")
				.param("status", "SCHEDULED")
				.param("page", "0").param("size", "1")
				.with(jwt().jwt(token -> token.subject(fixture.coachId().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(fixture.sessionId().toString()));

		mvc.perform(get("/coach/class-sessions/{id}", fixture.sessionId())
				.with(jwt().jwt(token -> token.subject(fixture.coachId().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members.length()").value(1))
			.andExpect(jsonPath("$.members[0].memberId").value(fixture.memberId().toString()));

		mvc.perform(get("/coach/class-sessions/{id}", fixture.sessionId())
				.with(jwt().jwt(token -> token.subject(fixture.otherCoachId().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));

		mvc.perform(get("/coach/class-sessions")
				.with(jwt().jwt(token -> token.subject(fixture.memberId().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden());
	}

	private Fixture fixture() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach A");
		Account otherCoach = saveAccount(AccountRole.COACH, "Coach B");
		Account member = saveAccount(AccountRole.MEMBER, "Member");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Yoga", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.YOGA, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio US37 A", 20, RoomStatus.ACTIVE));
		Room otherRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio US37 B", 20,
			RoomStatus.ACTIVE));
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			room, Instant.parse("2030-01-01T02:00:00Z"), Instant.parse("2030-01-01T03:00:00Z"), 12,
			manager.getId()));
		sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, otherCoach, otherRoom,
			Instant.parse("2030-01-01T02:00:00Z"), Instant.parse("2030-01-01T03:00:00Z"), 12,
			manager.getId()));
		createBookedAndCancelledMembers(manager.getId(), member, session.getId());
		return new Fixture(coach.getId(), otherCoach.getId(), member.getId(), session.getId());
	}

	private void createBookedAndCancelledMembers(UUID managerId, Account bookedMember, UUID sessionId) {
		Account cancelledMember = saveAccount(AccountRole.MEMBER, "Cancelled Member");
		jdbc.update("insert into member_profiles(account_id, profile_image_url) values (?, ?)",
			bookedMember.getId(), "https://example.test/booked.jpg");
		jdbc.update("insert into member_profiles(account_id, profile_image_url) values (?, ?)",
			cancelledMember.getId(), "https://example.test/cancelled.jpg");
		UUID offerId = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id, plan_code, name, description, price_amount, "
			+ "currency_code, duration_days, status, created_by_account_id) values (?, 'PLUS', ?, ?, 12000, "
			+ "'VND', 365, 'ACTIVE', ?)", offerId, "US37 Plus", "US37 integration offer", managerId);
		UUID bookedMembershipId = createMembership(bookedMember.getId(), offerId);
		UUID cancelledMembershipId = createMembership(cancelledMember.getId(), offerId);
		jdbc.update("insert into bookings(id, class_session_id, member_account_id, membership_id, status, "
			+ "booked_by_account_id) values (?, ?, ?, ?, 'BOOKED', ?)", UUID.randomUUID(), sessionId,
			bookedMember.getId(), bookedMembershipId, bookedMember.getId());
		jdbc.update("insert into bookings(id, class_session_id, member_account_id, membership_id, status, "
			+ "booked_by_account_id, cancelled_by_account_id, cancellation_source, cancelled_at) "
			+ "values (?, ?, ?, ?, 'CANCELLED', ?, ?, 'MEMBER', current_timestamp)", UUID.randomUUID(),
			sessionId, cancelledMember.getId(), cancelledMembershipId, cancelledMember.getId(),
			cancelledMember.getId());
	}

	private UUID createMembership(UUID memberId, UUID offerId) {
		UUID orderId = UUID.randomUUID();
		jdbc.update("insert into membership_orders(id, order_number, member_account_id, created_by_account_id, "
			+ "offer_id, offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, "
			+ "duration_days_snapshot, payment_method, status, expires_at, paid_at) values (?, ?, ?, ?, ?, ?, "
			+ "'PLUS', 12000, 'VND', 365, 'CASH', 'PAID', current_timestamp + interval '1 day', current_timestamp)",
			orderId, "US37-" + orderId.toString().replace("-", ""), memberId, memberId, offerId, "US37 Plus");
		jdbc.update("insert into payments(id, order_id, method, status, amount, currency_code, "
			+ "processed_by_account_id, paid_at) values (?, ?, 'CASH', 'PAID', 12000, 'VND', ?, "
			+ "current_timestamp)", UUID.randomUUID(), orderId, memberId);
		UUID membershipId = UUID.randomUUID();
		jdbc.update("insert into memberships(id, member_account_id, order_id, offer_id, plan_code_snapshot, "
			+ "offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, status, "
			+ "starts_at, ends_at) values (?, ?, ?, ?, 'PLUS', ?, 12000, 'VND', 365, 'ACTIVE', "
			+ "'2029-01-01T00:00:00Z', '2031-01-01T00:00:00Z')", membershipId, memberId, orderId, offerId,
			"US37 Plus");
		return membershipId;
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private record Fixture(UUID coachId, UUID otherCoachId, UUID memberId, UUID sessionId) { }
}
