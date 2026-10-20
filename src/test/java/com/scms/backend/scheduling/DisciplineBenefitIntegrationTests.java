package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.hibernate.SessionFactory;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DisciplineBenefitIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mvc;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;
	@Autowired ObjectMapper objectMapper;
	@Autowired DisciplineService disciplineService;
	@Autowired BookingService bookingService;
	@Autowired DisciplineRepository disciplineRepository;
	@Autowired SportClassRepository classes;
	@Autowired ClassSessionRepository sessions;
	@Autowired RoomRepository rooms;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;

	@Test
	@Transactional
	void createPatchGetAndListUseEffectiveSortedBenefitsAndExistingJoinTable() throws Exception {
		Account manager = manager();
		MvcResult created = mvc.perform(post("/manager/disciplines")
				.with(managerJwt(manager.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Aquatics","requiredBenefitCodes":[" pool_access ","CLASS_BOOKING","massage_access"]}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.requiredBenefitCodes[0]").value("CLASS_BOOKING"))
			.andExpect(jsonPath("$.requiredBenefitCodes[1]").value("MASSAGE_ACCESS"))
			.andExpect(jsonPath("$.requiredBenefitCodes[2]").value("POOL_ACCESS"))
			.andReturn();
		UUID disciplineId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
			.path("id").asText());

		assertThat(jdbc.queryForList(
			"select benefit_code from discipline_required_benefits where discipline_id=? order by benefit_code",
			String.class, disciplineId)).containsExactly("MASSAGE_ACCESS", "POOL_ACCESS");
		assertThat(auditBenefits("DISCIPLINE_CREATED", disciplineId))
			.containsExactly("CLASS_BOOKING", "MASSAGE_ACCESS", "POOL_ACCESS");

		mvc.perform(patch("/manager/disciplines/{disciplineId}", disciplineId)
				.with(managerJwt(manager.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"Updated\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requiredBenefitCodes[2]").value("POOL_ACCESS"));
		mvc.perform(get("/manager/disciplines/{disciplineId}", disciplineId)
				.with(managerJwt(manager.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requiredBenefitCodes[0]").value("CLASS_BOOKING"))
			.andExpect(jsonPath("$.requiredBenefitCodes[1]").value("MASSAGE_ACCESS"))
			.andExpect(jsonPath("$.requiredBenefitCodes[2]").value("POOL_ACCESS"));

		JsonNode listed = objectMapper.readTree(mvc.perform(get("/manager/disciplines")
				.with(managerJwt(manager.getId())))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		JsonNode listedDiscipline = java.util.stream.StreamSupport.stream(listed.spliterator(), false)
			.filter(item -> disciplineId.toString().equals(item.path("id").asText())).findFirst().orElseThrow();
		assertThat(objectMapper.convertValue(listedDiscipline.path("requiredBenefitCodes"), List.class))
			.containsExactly("CLASS_BOOKING", "MASSAGE_ACCESS", "POOL_ACCESS");

		mvc.perform(patch("/manager/disciplines/{disciplineId}", disciplineId)
				.with(managerJwt(manager.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requiredBenefitCodes\":[]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requiredBenefitCodes[0]").value("CLASS_BOOKING"))
			.andExpect(jsonPath("$.requiredBenefitCodes.length()").value(1));
		assertThat(jdbc.queryForObject(
			"select count(*) from discipline_required_benefits where discipline_id=?", Long.class, disciplineId))
			.isZero();
		assertThat(auditBenefitSnapshots("DISCIPLINE_UPDATED", disciplineId))
			.anySatisfy(snapshot -> assertThat(snapshot).containsExactly("CLASS_BOOKING"));
	}

	@Test
	@Transactional
	void createNullOrEmptyBenefitsDefaultsToClassBooking() throws Exception {
		Account manager = manager();
		for (String body : List.of("{\"name\":\"No benefits field\"}",
			"{\"name\":\"Empty benefits\",\"requiredBenefitCodes\":[]}")) {
			mvc.perform(post("/manager/disciplines").with(managerJwt(manager.getId()))
					.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.requiredBenefitCodes[0]").value("CLASS_BOOKING"))
				.andExpect(jsonPath("$.requiredBenefitCodes.length()").value(1));
		}
	}

	@Test
	@Transactional
	void createAndPatchRejectInvalidBenefitCodesOnTheStableField() throws Exception {
		Account manager = manager();
		jdbc.update("update membership_benefits set status='INACTIVE' where code='MASSAGE_ACCESS'");
		List<String> invalidBodies = List.of(
			"{\"name\":\"Null entry\",\"requiredBenefitCodes\":[null]}",
			"{\"name\":\"Blank entry\",\"requiredBenefitCodes\":[\"  \"]}",
			"{\"name\":\"Duplicate entry\",\"requiredBenefitCodes\":[\"pool_access\",\" POOL_ACCESS \"]}",
			"{\"name\":\"Unknown entry\",\"requiredBenefitCodes\":[\"UNKNOWN\"]}",
			"{\"name\":\"Inactive entry\",\"requiredBenefitCodes\":[\"massage_access\"]}");
		for (String body : invalidBodies) {
			mvc.perform(post("/manager/disciplines").with(managerJwt(manager.getId()))
					.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors.requiredBenefitCodes[0]").exists());
		}

		MvcResult created = mvc.perform(post("/manager/disciplines").with(managerJwt(manager.getId()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Patch target\"}"))
			.andExpect(status().isCreated()).andReturn();
		UUID disciplineId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
			.path("id").asText());
		mvc.perform(patch("/manager/disciplines/{disciplineId}", disciplineId)
				.with(managerJwt(manager.getId())).contentType(MediaType.APPLICATION_JSON)
				.content("{\"requiredBenefitCodes\":[\"pool_access\",\"POOL_ACCESS\"]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.requiredBenefitCodes[0]").exists());
	}

	@Test
	void bookingWaitsForRequiredBenefitPatchAndUsesCommittedRequirements() throws Exception {
		BookingFixture fixture = bookingFixture();
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		CountDownLatch patchHoldingDisciplineLock = new CountDownLatch(1);
		CountDownLatch releasePatch = new CountDownLatch(1);
		CountDownLatch bookingStarted = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var patch = executor.submit(() -> transactions.executeWithoutResult(status -> {
				disciplineService.update(fixture.managerId(), fixture.disciplineId(),
					new DisciplinePatchRequest(null, null, null, List.of("POOL_ACCESS")));
				patchHoldingDisciplineLock.countDown();
				await(releasePatch);
			}));
			assertThat(patchHoldingDisciplineLock.await(10, TimeUnit.SECONDS)).isTrue();

			var booking = executor.submit(() -> {
				bookingStarted.countDown();
				try {
					transactions.executeWithoutResult(status ->
						bookingService.book(fixture.memberId(), fixture.sessionId()));
					return "BOOKED";
				}
				catch (BookingException exception) {
					assertThat(exception.getRequiredBenefitCodes())
						.containsExactly("CLASS_BOOKING", "POOL_ACCESS");
					assertThat(exception.getMissingBenefitCodes()).containsExactly("POOL_ACCESS");
					return exception.getCode();
				}
			});
			assertThat(bookingStarted.await(10, TimeUnit.SECONDS)).isTrue();
			Thread.sleep(250);
			assertThat(booking).as("booking must wait for the Discipline row lock").isNotDone();
			releasePatch.countDown();

			patch.get(10, TimeUnit.SECONDS);
			assertThat(booking.get(10, TimeUnit.SECONDS)).isEqualTo("MEMBERSHIP_BENEFIT_REQUIRED");
		}
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id=?", Long.class,
			fixture.sessionId())).isZero();
	}

	@Test
	void bookingsForDifferentSessionsUnderOneDisciplineHoldSharedLocksConcurrently() throws Exception {
		ParallelBookingFixture fixture = parallelBookingFixture();
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		CountDownLatch bothBookingsHoldingLocks = new CountDownLatch(2);
		CountDownLatch releaseBookings = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> transactions.executeWithoutResult(status -> {
				bookingService.book(fixture.firstMemberId(), fixture.firstSessionId());
				bothBookingsHoldingLocks.countDown();
				await(releaseBookings);
			}));
			var second = executor.submit(() -> transactions.executeWithoutResult(status -> {
				bookingService.book(fixture.secondMemberId(), fixture.secondSessionId());
				bothBookingsHoldingLocks.countDown();
				await(releaseBookings);
			}));
			boolean bothReachedLockBoundary;
			try {
				bothReachedLockBoundary = bothBookingsHoldingLocks.await(10, TimeUnit.SECONDS);
			}
			finally {
				releaseBookings.countDown();
			}
			assertThat(bothReachedLockBoundary)
				.as("shared Discipline locks must not serialize bookings for different Sessions").isTrue();
			first.get(10, TimeUnit.SECONDS);
			second.get(10, TimeUnit.SECONDS);
		}
		assertThat(jdbc.queryForObject("select count(*) from bookings where class_session_id in (?, ?)", Long.class,
			fixture.firstSessionId(), fixture.secondSessionId())).isEqualTo(2);
	}

	@Test
	void managerListLoadsAllRequiredBenefitsWithoutPerDisciplineQueries() {
		Account manager = manager();
		for (String name : List.of("Aquatics " + UUID.randomUUID(), "Recovery " + UUID.randomUUID())) {
			Discipline discipline = new Discipline(UUID.randomUUID(), name, null, DisciplineStatus.ACTIVE);
			discipline.replaceRequiredBenefitCodes(List.of("POOL_ACCESS"));
			disciplineRepository.saveAndFlush(discipline);
		}
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		var statistics = sessionFactory.getStatistics();
		boolean wasEnabled = statistics.isStatisticsEnabled();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		try {
			assertThat(disciplineService.list(manager.getId())).hasSizeGreaterThanOrEqualTo(2);
			assertThat(statistics.getPrepareStatementCount())
				.as("manager authorization plus one set-based Discipline query").isEqualTo(2);
		}
		finally {
			statistics.setStatisticsEnabled(wasEnabled);
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> auditBenefits(String action, UUID disciplineId) throws Exception {
		return auditBenefitSnapshots(action, disciplineId).getFirst();
	}

	@SuppressWarnings("unchecked")
	private List<List<String>> auditBenefitSnapshots(String action, UUID disciplineId) throws Exception {
		List<List<String>> snapshots = new ArrayList<>();
		for (String json : jdbc.queryForList("select after_data ->> 'requiredBenefitCodes' from audit_events "
				+ "where action=? and target_id=?", String.class, action, disciplineId)) {
			snapshots.add(objectMapper.readValue(json, List.class));
		}
		return snapshots;
	}

	private Account manager() {
		return account(AccountRole.MANAGER, "Manager");
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private BookingFixture bookingFixture() {
		Account manager = manager();
		Account member = account(AccountRole.MEMBER, "Member");
		Account coach = account(AccountRole.COACH, "Coach");
		Discipline discipline = disciplineRepository.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Booking lock " + UUID.randomUUID(), null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Lock class",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Lock room " + UUID.randomUUID(), 10,
			RoomStatus.ACTIVE));
		Instant startsAt = Instant.now().plusSeconds(86_400);
		ClassSession session = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			room, startsAt, startsAt.plusSeconds(3600), 10, manager.getId()));
		createPlusMembership(manager.getId(), member.getId(), startsAt);
		return new BookingFixture(manager.getId(), member.getId(), discipline.getId(), session.getId());
	}

	private ParallelBookingFixture parallelBookingFixture() {
		Account manager = manager();
		Account firstMember = account(AccountRole.MEMBER, "First member");
		Account secondMember = account(AccountRole.MEMBER, "Second member");
		Account firstCoach = account(AccountRole.COACH, "First coach");
		Account secondCoach = account(AccountRole.COACH, "Second coach");
		Discipline discipline = disciplineRepository.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Parallel booking " + UUID.randomUUID(), null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Parallel class",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room firstRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "First room " + UUID.randomUUID(), 10,
			RoomStatus.ACTIVE));
		Room secondRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Second room " + UUID.randomUUID(), 10,
			RoomStatus.ACTIVE));
		Instant startsAt = Instant.now().plusSeconds(86_400);
		ClassSession firstSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null,
			firstCoach, firstRoom, startsAt, startsAt.plusSeconds(3600), 10, manager.getId()));
		ClassSession secondSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null,
			secondCoach, secondRoom, startsAt, startsAt.plusSeconds(3600), 10, manager.getId()));
		createPlusMembership(manager.getId(), firstMember.getId(), startsAt);
		createPlusMembership(manager.getId(), secondMember.getId(), startsAt);
		return new ParallelBookingFixture(firstMember.getId(), firstSession.getId(), secondMember.getId(),
			secondSession.getId());
	}

	private void createPlusMembership(UUID managerId, UUID memberId, Instant sessionStart) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id, plan_code, name, price_amount, currency_code, "
			+ "duration_days, status, created_by_account_id) values (?, 'PLUS', ?, 12000, 'VND', 30, "
			+ "'ACTIVE', ?)", offerId, "Lock offer " + offerId, managerId);
		jdbc.update("insert into membership_orders(id, order_number, member_account_id, created_by_account_id, "
			+ "offer_id, offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, "
			+ "duration_days_snapshot, payment_method, status, expires_at, paid_at) values (?, ?, ?, ?, ?, ?, "
			+ "'PLUS', 12000, 'VND', 30, 'CASH', 'PAID', ?, ?)", orderId,
			"LOCK-" + orderId.toString().replace("-", ""), memberId, managerId, offerId, "Lock offer",
			java.sql.Timestamp.from(sessionStart.plusSeconds(3600)), java.sql.Timestamp.from(Instant.now()));
		jdbc.update("insert into payments(id, order_id, method, status, amount, currency_code, "
			+ "processed_by_account_id, paid_at) values (?, ?, 'CASH', 'PAID', 12000, 'VND', ?, ?)",
			UUID.randomUUID(), orderId, managerId, java.sql.Timestamp.from(Instant.now()));
		jdbc.update("insert into memberships(id, member_account_id, order_id, offer_id, plan_code_snapshot, "
			+ "offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, status, "
			+ "starts_at, ends_at) values (?, ?, ?, ?, 'PLUS', 'Lock offer', 12000, 'VND', 30, 'ACTIVE', ?, ?)",
			UUID.randomUUID(), memberId, orderId, offerId, java.sql.Timestamp.from(sessionStart.minusSeconds(3600)),
			java.sql.Timestamp.from(sessionStart.plusSeconds(86_400)));
	}

	private void await(CountDownLatch latch) {
		try {
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}

	private record BookingFixture(UUID managerId, UUID memberId, UUID disciplineId, UUID sessionId) { }

	private record ParallelBookingFixture(UUID firstMemberId, UUID firstSessionId, UUID secondMemberId,
		UUID secondSessionId) { }

	private org.springframework.test.web.servlet.request.RequestPostProcessor managerJwt(UUID accountId) {
		return jwt().jwt(token -> token.subject(accountId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
	}
}
