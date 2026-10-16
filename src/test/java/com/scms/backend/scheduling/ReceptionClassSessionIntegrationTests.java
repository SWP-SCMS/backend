package com.scms.backend.scheduling;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ReceptionClassSessionIntegrationTests.FixedClockConfiguration.class)
class ReceptionClassSessionIntegrationTests {

	private static final Instant NOW = Instant.parse("2030-01-01T03:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mvc;
	@Autowired AccountRepository accounts;
	@Autowired ClassSessionRepository sessions;
	@Autowired BookingRepository bookings;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;

	@Test
	@Transactional
	void catalogueUsesFutureHalfOpenFiltersStablePagingAndLiveCapacity() throws Exception {
		Fixture fixture = fixture();

		mvc.perform(get("/reception/class-sessions")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.content[0].id").value(fixture.openSession().getId().toString()))
			.andExpect(jsonPath("$.content[0].className").value("Vinyasa"))
			.andExpect(jsonPath("$.content[0].disciplineName").value("Yoga"))
			.andExpect(jsonPath("$.content[0].coachName").value("Coach"))
			.andExpect(jsonPath("$.content[0].roomName").value("Studio"))
			.andExpect(jsonPath("$.content[0].bookedCount").value(0))
			.andExpect(jsonPath("$.content[0].availableCapacity").value(2))
			.andExpect(jsonPath("$.content[0].bookingAvailable").value(true))
			.andExpect(jsonPath("$.content[1].id").value(fixture.fullSession().getId().toString()))
			.andExpect(jsonPath("$.content[1].bookedCount").value(1))
			.andExpect(jsonPath("$.content[1].availableCapacity").value(0))
			.andExpect(jsonPath("$.content[1].bookingAvailable").value(false));

		mvc.perform(get("/reception/class-sessions")
				.param("from", "2030-01-01T05:00:00Z")
				.param("to", "2030-01-01T06:00:00Z")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(fixture.fullSession().getId().toString()));

		mvc.perform(get("/reception/class-sessions")
				.param("classId", fixture.otherClass().getId().toString())
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(fixture.otherClassSession().getId().toString()));

		mvc.perform(get("/reception/class-sessions")
				.param("page", "1").param("size", "1")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(1))
			.andExpect(jsonPath("$.content[0].id").value(fixture.fullSession().getId().toString()));

		bookings.saveAndFlush(new Booking(UUID.randomUUID(), fixture.openSession().getId(),
			fixture.dynamicMember().getId(), fixture.dynamicMembershipId(), fixture.receptionist().getId(), NOW));

		mvc.perform(get("/reception/class-sessions")
				.param("from", "2030-01-01T04:00:00Z")
				.param("to", "2030-01-01T05:00:00Z")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].bookedCount").value(1))
			.andExpect(jsonPath("$.content[0].availableCapacity").value(1))
			.andExpect(jsonPath("$.content[0].bookingAvailable").value(true));
	}

	@Test
	@Transactional
	void endpointEnforcesRoleActiveAccountAndExistingValidationContract() throws Exception {
		Fixture fixture = fixture();

		mvc.perform(get("/reception/class-sessions"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		for (Account actor : new Account[] { fixture.member(), fixture.coach(), fixture.manager() }) {
			mvc.perform(get("/reception/class-sessions")
					.with(jwt().jwt(token -> token.subject(actor.getId().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_" + actor.getRole().name()))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}

		mvc.perform(get("/reception/class-sessions")
				.with(receptionistJwt(fixture.inactiveReceptionist().getId())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

		mvc.perform(get("/reception/class-sessions")
				.param("from", "2030-01-01T05:00:00Z")
				.param("to", "2030-01-01T05:00:00Z")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.to[0]").value("must be later than from"));

		mvc.perform(get("/reception/class-sessions")
				.param("classId", "not-a-uuid")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

		mvc.perform(get("/reception/class-sessions")
				.param("size", "101")
				.with(receptionistJwt(fixture.receptionist().getId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor receptionistJwt(UUID accountId) {
		return jwt().jwt(token -> token.subject(accountId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"));
	}

	private Fixture fixture() {
		Account manager = saveAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager");
		Account coach = saveAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach");
		Account receptionist = saveAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE, "Receptionist");
		Account inactiveReceptionist = saveAccount(AccountRole.RECEPTIONIST, AccountStatus.INACTIVE,
			"Inactive Receptionist");
		Account member = saveAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Member");
		Account cancelledMember = saveAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Cancelled Member");
		Account dynamicMember = saveAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Dynamic Member");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Yoga", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.YOGA, null, SportClassStatus.ACTIVE));
		SportClass otherClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Strength",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE));

		sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach, room,
			NOW.minusSeconds(7200), NOW.minusSeconds(3600), 2, manager.getId()));
		ClassSession openSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null,
			coach, room, NOW.plusSeconds(3600), NOW.plusSeconds(7200), 2, manager.getId()));
		ClassSession fullSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null,
			coach, room, NOW.plusSeconds(7200), NOW.plusSeconds(10800), 1, manager.getId()));
		ClassSession otherClassSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), otherClass,
			null, coach, room, NOW.plusSeconds(10800), NOW.plusSeconds(14400), 3, manager.getId()));
		ClassSession cancelledSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass,
			null, coach, room, NOW.plusSeconds(14400), NOW.plusSeconds(18000), 2, manager.getId()));
		jdbc.update("""
			update class_sessions
			set status = 'CANCELLED', cancelled_at = ?, cancellation_reason = ?,
				updated_at = current_timestamp
			where id = ?
			""", java.sql.Timestamp.from(NOW), "Cancelled for test", cancelledSession.getId());

		UUID bookedMembershipId = createMembership(manager.getId(), member.getId(), "Booked");
		UUID cancelledMembershipId = createMembership(manager.getId(), cancelledMember.getId(), "Cancelled");
		UUID dynamicMembershipId = createMembership(manager.getId(), dynamicMember.getId(), "Dynamic");
		bookings.saveAndFlush(new Booking(UUID.randomUUID(), fullSession.getId(), member.getId(),
			bookedMembershipId, receptionist.getId(), NOW));
		Booking cancelledBooking = new Booking(UUID.randomUUID(), fullSession.getId(), cancelledMember.getId(),
			cancelledMembershipId, receptionist.getId(), NOW);
		cancelledBooking.cancel(cancelledMember.getId(), "MEMBER", NOW.plusSeconds(60));
		bookings.saveAndFlush(cancelledBooking);

		return new Fixture(manager, coach, receptionist, inactiveReceptionist, member, dynamicMember, openSession,
			fullSession, otherClass, otherClassSession, dynamicMembershipId);
	}

	private UUID createMembership(UUID managerId, UUID memberId, String suffix) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		insertMembershipRows(managerId, memberId, suffix, offerId, orderId, membershipId);
		return membershipId;
	}

	@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

	private void insertMembershipRows(UUID managerId, UUID memberId, String suffix, UUID offerId, UUID orderId,
			UUID membershipId) {
		jdbc.update("insert into membership_offers(id, plan_code, name, description, price_amount, "
			+ "currency_code, duration_days, status, created_by_account_id) values (?, 'PLUS', ?, ?, 12000, "
			+ "'VND', 365, 'ACTIVE', ?)", offerId, "US35 " + suffix, "US35 test offer", managerId);
		jdbc.update("insert into membership_orders(id, order_number, member_account_id, created_by_account_id, "
			+ "offer_id, offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, "
			+ "duration_days_snapshot, payment_method, status, expires_at, paid_at) values (?, ?, ?, ?, ?, ?, "
			+ "'PLUS', 12000, 'VND', 365, 'CASH', 'PAID', ?, ?)", orderId,
			"US35-" + orderId.toString().replace("-", ""), memberId, managerId, offerId, "US35 " + suffix,
			java.sql.Timestamp.from(Instant.parse("2030-01-02T00:00:00Z")), java.sql.Timestamp.from(NOW));
		jdbc.update("insert into memberships(id, member_account_id, order_id, offer_id, plan_code_snapshot, "
			+ "offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, status, "
			+ "starts_at, ends_at) values (?, ?, ?, ?, 'PLUS', ?, 12000, 'VND', 365, 'ACTIVE', "
			+ "'2029-01-01T00:00:00Z', '2031-01-01T00:00:00Z')", membershipId, memberId, orderId, offerId,
			"US35 " + suffix);
	}

	private Account saveAccount(AccountRole role, AccountStatus status, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, status, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private record Fixture(Account manager, Account coach, Account receptionist, Account inactiveReceptionist,
			Account member, Account dynamicMember, ClassSession openSession, ClassSession fullSession,
			SportClass otherClass, ClassSession otherClassSession, UUID dynamicMembershipId) { }

	@TestConfiguration
	static class FixedClockConfiguration {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}
}
