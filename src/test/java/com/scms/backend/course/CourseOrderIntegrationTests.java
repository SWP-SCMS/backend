package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseOrderIntegrationTests {

	private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mockMvc;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate db;
	@Autowired CourseWaitlistService waitlists;
	@Autowired JwtEncoder jwtEncoder;
	private final ObjectMapper objectMapper = new ObjectMapper();
	@MockitoBean Clock clock;
	@MockitoBean CourseOrderCreationRendezvous creationRendezvous;

	@BeforeEach
	void setUpClock() {
		when(clock.instant()).thenReturn(NOW);
	}

	@Test
	void createsAndReplaysTheSameCanonicalRequestButRejectsKeyReuse() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture first = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture second = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(10800));

		MvcResult created = createSelf(member, first.runId(), "same-key").andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(member.getAccountId().toString()))
			.andExpect(jsonPath("$.courseRunId").value(first.runId().toString()))
			.andExpect(jsonPath("$.orderReference").value(org.hamcrest.Matchers.matchesPattern("COR-[0-9A-F]{32}")))
			.andExpect(jsonPath("$.tuitionVnd").value(500000))
			.andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.reservationStatus").value("HELD"))
			.andExpect(jsonPath("$.expiresAt").value(NOW.plusSeconds(900).toString()))
			.andReturn();
		String original = created.getResponse().getContentAsString();

		assertThat(createSelf(member, first.runId(), " same-key ").andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString()).isEqualTo(original);
		createSelf(member, second.runId(), "same-key").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_IDEMPOTENCY_KEY_REUSED"));
		assertThat(count("course_orders", member.getAccountId())).isOne();
		assertThat(count("course_reservations", member.getAccountId())).isOne();
	}

	@Test
	void registrationWindowIsHalfOpenAndExpiryUsesTheEarlierDeadline() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture beforeOpen = run(3, NOW.plusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture atOpen = run(3, NOW, NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(10800));
		RunFixture beforeClose = run(3, NOW.minusSeconds(3600), NOW.plusSeconds(300), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));
		RunFixture atClose = run(3, NOW.minusSeconds(3600), NOW, "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(18000));
		RunFixture afterClose = run(3, NOW.minusSeconds(3600), NOW.minusSeconds(1), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(21600));

		createSelf(member, beforeOpen.runId(), "before-open").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_REGISTRATION_NOT_OPEN"));
		createSelf(member, atOpen.runId(), "at-open").andExpect(status().isCreated())
			.andExpect(jsonPath("$.expiresAt").value(NOW.plusSeconds(900).toString()));
		createSelf(member, beforeClose.runId(), "before-close").andExpect(status().isCreated())
			.andExpect(jsonPath("$.expiresAt").value(NOW.plusSeconds(300).toString()));
		createSelf(member, atClose.runId(), "at-close").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_REGISTRATION_CLOSED"));
		createSelf(member, afterClose.runId(), "after-close").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_REGISTRATION_CLOSED"));
	}

	@Test
	void rejectsHiddenNonOpenInactiveDuplicateScheduleConflictAndFullCapacity() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture hidden = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "INACTIVE",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture nonOpen = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED", "DRAFT",
			NOW.plusSeconds(10800));
		createSelf(member, hidden.runId(), "hidden").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_RUN_NOT_AVAILABLE"));
		createSelf(member, nonOpen.runId(), "non-open").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_RUN_NOT_AVAILABLE"));

		RunFixture held = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));
		createSelf(member, held.runId(), "held-1").andExpect(status().isCreated());
		createSelf(member, held.runId(), "held-2").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_RESERVATION_EXISTS"));

		RunFixture overlap = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", held.sessionStart().plusSeconds(1800));
		createSelf(member, overlap.runId(), "overlap").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_SCHEDULE_CONFLICT"));

		RunFixture full = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(28800));
		createSelf(member(AccountStatus.ACTIVE), full.runId(), "seat-1").andExpect(status().isCreated());
		createSelf(member(AccountStatus.ACTIVE), full.runId(), "seat-2").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_CAPACITY_FULL"));

		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile suspended = member(AccountStatus.SUSPENDED);
		createReception(receptionist, suspended, full.runId(), "suspended").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_ACTIVE"));
	}

	@Test
	void exactExpiryIsExpiredGetNeverExtendsAndNewCreateReplacesOnlyTheStaleHold() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		JsonNode created = json(createSelf(member, fixture.runId(), "expiry-1").andExpect(status().isCreated())
			.andReturn());
		UUID orderId = UUID.fromString(created.path("orderId").asText());
		Instant expiry = Instant.parse(created.path("expiresAt").asText());

		when(clock.instant()).thenReturn(expiry.minusNanos(1));
		getSelf(member, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationStatus").value("HELD"))
			.andExpect(jsonPath("$.expiresAt").value(expiry.toString()));
		when(clock.instant()).thenReturn(expiry);
		getSelf(member, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.orderStatus").value("EXPIRED"))
			.andExpect(jsonPath("$.reservationStatus").value("EXPIRED"))
			.andExpect(jsonPath("$.expiresAt").value(expiry.toString()));
		when(clock.instant()).thenReturn(expiry.plusSeconds(1));
		getSelf(member, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationStatus").value("EXPIRED"));

		createSelf(member, fixture.runId(), "expiry-2").andExpect(status().isCreated());
		assertThat(db.queryForObject("""
			select count(*) from course_reservations
			where member_account_id=? and course_run_id=? and status='HELD'
			""", Long.class, member.getAccountId(), fixture.runId())).isOne();
		assertThat(db.queryForObject("""
			select count(*) from course_reservations
			where member_account_id=? and course_run_id=? and status='EXPIRED'
			""", Long.class, member.getAccountId(), fixture.runId())).isOne();
	}

	@Test
	void invitedMemberAcceptsOnceAndSameIdempotencyKeyReplaysTheOrder() throws Exception {
		MemberProfile holder = member(AccountStatus.ACTIVE);
		MemberProfile waiting = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		createSelf(holder, fixture.runId(), "holder").andExpect(status().isCreated());
		UUID waitlistId = UUID.fromString(json(joinWaitlist(waiting, fixture.runId())
			.andExpect(status().isCreated()).andReturn()).path("id").asText());
		db.update("update course_reservations set status='EXPIRED' where member_account_id=? and course_run_id=?",
			holder.getAccountId(), fixture.runId());
		waitlists.promote(fixture.runId(), NOW);
		waitlists.promote(fixture.runId(), NOW);

		MvcResult accepted = acceptWaitlist(waiting, waitlistId, "accept-once")
			.andExpect(status().isCreated()).andReturn();
		String body = accepted.getResponse().getContentAsString();
		assertThat(acceptWaitlist(waiting, waitlistId, "accept-once").andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString()).isEqualTo(body);
		assertThat(db.queryForObject("select count(*) from course_orders where member_account_id=? and course_run_id=?",
			Long.class, waiting.getAccountId(), fixture.runId())).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_WAITLIST_ACCEPTED' and entity_id=?",
			Long.class, waitlistId)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where event_key=?",
			Long.class, "course-waitlist-invited:" + waitlistId)).isOne();
	}

	@Test
	void acceptedInvitationRetryReturnsItsOrderAfterReservationExpiresAndSeatIsTaken() throws Exception {
		MemberProfile holder = member(AccountStatus.ACTIVE);
		MemberProfile acceptedMember = member(AccountStatus.ACTIVE);
		MemberProfile nextMember = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		createSelf(holder, fixture.runId(), "expiry-holder").andExpect(status().isCreated());
		UUID waitlistId = UUID.fromString(json(joinWaitlist(acceptedMember, fixture.runId())
			.andExpect(status().isCreated()).andReturn()).path("id").asText());
		db.update("update course_reservations set status='EXPIRED' where member_account_id=? and course_run_id=?",
			holder.getAccountId(), fixture.runId());
		waitlists.promote(fixture.runId(), NOW);

		JsonNode accepted = json(acceptWaitlist(acceptedMember, waitlistId, "accept-original")
			.andExpect(status().isCreated()).andReturn());
		UUID acceptedOrderId = UUID.fromString(accepted.path("orderId").asText());
		when(clock.instant()).thenReturn(NOW.plusSeconds(901));
		createSelf(nextMember, fixture.runId(), "take-expired-seat").andExpect(status().isCreated());

		acceptWaitlist(acceptedMember, waitlistId, "accept-retry")
			.andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value(acceptedOrderId.toString()));
		assertThat(db.queryForObject("select accepted_order_id from course_waitlists where id=?", UUID.class,
			waitlistId)).isEqualTo(acceptedOrderId);
		assertThat(db.queryForObject("select count(*) from course_orders where course_run_id=?", Long.class,
			fixture.runId())).isEqualTo(3L);
		assertThat(db.queryForObject("""
			select count(*) from course_reservations
			where course_run_id=? and (status='ENROLLED' or (status='HELD' and expires_at>?))
			""", Long.class, fixture.runId(), Timestamp.from(NOW.plusSeconds(901)))).isOne();
	}

	@Test
	void leavingWaitlistDefersTheNextInvitationToScheduledPromotion() throws Exception {
		MemberProfile holder = member(AccountStatus.ACTIVE);
		MemberProfile leaving = member(AccountStatus.ACTIVE);
		MemberProfile next = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		createSelf(holder, fixture.runId(), "leave-holder").andExpect(status().isCreated());
		UUID leavingId = UUID.fromString(json(joinWaitlist(leaving, fixture.runId())
			.andExpect(status().isCreated()).andReturn()).path("id").asText());
		UUID nextId = UUID.fromString(json(joinWaitlist(next, fixture.runId())
			.andExpect(status().isCreated()).andReturn()).path("id").asText());

		mockMvc.perform(delete("/api/v1/course-runs/{runId}/waitlist", fixture.runId()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(leaving.getAccount())))
			.andExpect(status().isOk());
		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, leavingId))
			.isEqualTo("LEFT");
		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, nextId))
			.isEqualTo("WAITING");

		waitlists.promote(fixture.runId(), NOW);
		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, nextId))
			.isEqualTo("INVITED");
	}

	@Test
	void cancellingCourseRunCancelsAndNotifiesItsActiveWaitlistInvitation() throws Exception {
		Account manager = account(AccountRole.MANAGER, AccountStatus.ACTIVE);
		MemberProfile holder = member(AccountStatus.ACTIVE);
		MemberProfile invited = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		JsonNode order = json(createSelf(holder, fixture.runId(), "cancel-holder")
			.andExpect(status().isCreated()).andReturn());
		UUID orderId = UUID.fromString(order.path("orderId").asText());
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders/{orderId}/payments/cash",
			holder.getAccountId(), orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "cancel-holder-cash"))
			.andExpect(status().isOk());
		UUID waitlistId = UUID.fromString(json(joinWaitlist(invited, fixture.runId())
			.andExpect(status().isCreated()).andReturn()).path("id").asText());
		assertThat(db.update("update course_reservations set status='EXPIRED' where course_order_id=?", orderId)).isOne();
		waitlists.promote(fixture.runId(), NOW);
		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, waitlistId))
			.isEqualTo("INVITED");

		mockMvc.perform(post("/api/v1/manager/course-runs/{runId}/cancel", fixture.runId())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager))
			.contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
			.andExpect(status().isOk());

		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, waitlistId))
			.isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select count(*) from notification_outbox where event_key=?",
			Long.class, "course-waitlist-cancelled:" + waitlistId)).isOne();
	}

	@Test
	void invitationIsExpiredAtTheExactBoundaryAndReleasesTheSeat() throws Exception {
		MemberProfile holder = member(AccountStatus.ACTIVE);
		MemberProfile waiting = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		createSelf(holder, fixture.runId(), "boundary-holder").andExpect(status().isCreated());
		UUID waitlistId = UUID.fromString(json(joinWaitlist(waiting, fixture.runId())
			.andExpect(status().isCreated()).andReturn()).path("id").asText());
		db.update("update course_reservations set status='EXPIRED' where member_account_id=? and course_run_id=?",
			holder.getAccountId(), fixture.runId());
		waitlists.promote(fixture.runId(), NOW);
		Instant expiresAt = db.queryForObject("select invitation_expires_at from course_waitlists where id=?",
			Timestamp.class, waitlistId).toInstant();
		when(clock.instant()).thenReturn(expiresAt);

		acceptWaitlist(waiting, waitlistId, "at-expiry").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_WAITLIST_INVITATION_EXPIRED"));
		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, waitlistId))
			.isEqualTo("EXPIRED");
	}

	@Test
	void paidEnrollmentAndItsEnrolledReservationConsumeOnlyOneCourseSeat() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile paidMember = member(AccountStatus.ACTIVE);
		MemberProfile anotherMember = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		JsonNode order = json(createSelf(paidMember, fixture.runId(), "paid-seat")
			.andExpect(status().isCreated()).andReturn());
		UUID orderId = UUID.fromString(order.path("orderId").asText());
		mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders/{orderId}/payments/cash",
			paidMember.getAccountId(), orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "paid-seat-cash"))
			.andExpect(status().isOk());

		joinWaitlist(anotherMember, fixture.runId()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_WAITLIST_CAPACITY_AVAILABLE"));
		createSelf(anotherMember, fixture.runId(), "second-seat").andExpect(status().isCreated());
	}

	@Test
	void getIsOwnershipScopedForMemberAndReceptionist() throws Exception {
		MemberProfile owner = member(AccountStatus.ACTIVE);
		MemberProfile other = member(AccountStatus.ACTIVE);
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		UUID orderId = UUID.fromString(json(createSelf(owner, fixture.runId(), "owner-key")
			.andExpect(status().isCreated()).andReturn()).path("orderId").asText());

		getSelf(other, orderId).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_ORDER_NOT_FOUND"));
		getReception(receptionist, other, orderId).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_ORDER_NOT_FOUND"));
		getReception(receptionist, owner, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(owner.getAccountId().toString()));
	}

	@Test
	void receptionistStoresDistinctActorAndIdempotencyKeysAreScopedPerActor() throws Exception {
		Account firstReceptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		Account secondReceptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile firstMember = member(AccountStatus.ACTIVE);
		MemberProfile secondMember = member(AccountStatus.ACTIVE);
		RunFixture firstRun = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture mismatchRun = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(10800));
		RunFixture secondActorRun = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));

		JsonNode created = json(createReception(firstReceptionist, firstMember, firstRun.runId(), "shared-key")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(firstMember.getAccountId().toString()))
			.andReturn());
		UUID orderId = UUID.fromString(created.path("orderId").asText());
		assertThat(db.queryForObject("select actor_account_id from course_orders where id=?", UUID.class, orderId))
			.isEqualTo(firstReceptionist.getId()).isNotEqualTo(firstMember.getAccountId());

		createReception(firstReceptionist, firstMember, firstRun.runId(), "shared-key")
			.andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value(orderId.toString()));
		createReception(firstReceptionist, firstMember, mismatchRun.runId(), "shared-key")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_IDEMPOTENCY_KEY_REUSED"));
		createReception(secondReceptionist, secondMember, secondActorRun.runId(), "shared-key")
			.andExpect(status().isCreated());

		assertThat(db.queryForObject("select count(*) from course_orders where idempotency_key='shared-key'",
			Long.class)).isEqualTo(2);
	}

	@Test
	void rejectsOverlapWithARealBookedGymSession() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		GymSessionWindow gymSession = bookGymSession(member);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", gymSession.startAt());
		assertThat(fixture.sessionStart()).isEqualTo(gymSession.startAt());
		assertThat(fixture.sessionStart().plusSeconds(3600)).isEqualTo(gymSession.endAt());

		createSelf(member, fixture.runId(), "booked-overlap").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_SCHEDULE_CONFLICT"));
	}

	@Test
	void concurrentSameKeyCreatesOnceAndFinalSeatCannotOversell() throws Exception {
		MemberProfile sameMember = member(AccountStatus.ACTIVE);
		RunFixture replayRun = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		List<MvcResult> replayResults = concurrent(
			() -> createSelf(sameMember, replayRun.runId(), "race-key").andReturn(),
			() -> createSelf(sameMember, replayRun.runId(), "race-key").andReturn());
		assertThat(replayResults).extracting(result -> result.getResponse().getStatus())
			.containsExactlyInAnyOrder(200, 201);
		assertThat(replayResults.get(0).getResponse().getContentAsString())
			.isEqualTo(replayResults.get(1).getResponse().getContentAsString());
		assertThat(count("course_orders", sameMember.getAccountId())).isOne();

		assertFinalSeatSerializesBehindCourseRunLock();
	}

	@Test
	void auditFailureRollsBackOrderReservationAndAudit() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		String constraint = "chk_test_reject_course_order_audit";
		db.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_ORDER_CREATED' or actor_account_id <> '"
			+ member.getAccountId() + "') not valid");
		try {
			createSelf(member, fixture.runId(), "rollback").andExpect(status().isInternalServerError());
			assertThat(count("course_orders", member.getAccountId())).isZero();
			assertThat(count("course_reservations", member.getAccountId())).isZero();
			assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_ORDER_CREATED'",
				Long.class)).isZero();
		}
		finally {
			db.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	@Test
	void missingKeyAndClientControlledFieldsAreRejectedAndRoleRulesApply() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		createSelf(member, fixture.runId(), null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COURSE_IDEMPOTENCY_KEY_REQUIRED"));
		mockMvc.perform(post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount()))
			.header("Idempotency-Key", "client-fields")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + fixture.runId() + "\",\"expiresAt\":\"2030-01-01T00:00:00Z\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		mockMvc.perform(post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(coach))
			.header("Idempotency-Key", "coach")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + fixture.runId() + "\"}"))
			.andExpect(status().isForbidden());
		String activeToken = token(member.getAccount());
		db.update("update accounts set status='SUSPENDED' where id=?", member.getAccountId());
		mockMvc.perform(post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + activeToken)
			.header("Idempotency-Key", "suspended-actor")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + fixture.runId() + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	private void assertFinalSeatSerializesBehindCourseRunLock() throws Exception {
		MemberProfile first = member(AccountStatus.ACTIVE);
		MemberProfile second = member(AccountStatus.ACTIVE);
		RunFixture lastSeat = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));
		CountDownLatch firstLocked = new CountDownLatch(1), secondAtLock = new CountDownLatch(1),
			releaseFirst = new CountDownLatch(1);
		AtomicInteger firstPid = new AtomicInteger(), secondPid = new AtomicInteger();
		doAnswer(invocation -> {
			if (Thread.currentThread().getName().equals("course-order-first-seat")) {
				firstPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
			}
			else if (Thread.currentThread().getName().equals("course-order-second-seat")) {
				secondPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				secondAtLock.countDown();
			}
			return null;
		}).when(creationRendezvous).beforeRunLock();
		doAnswer(invocation -> {
			if (Thread.currentThread().getName().equals("course-order-first-seat")) {
				firstLocked.countDown();
				await(releaseFirst);
			}
			return null;
		}).when(creationRendezvous).afterRunLock();

		var executor = Executors.newFixedThreadPool(2);
		try {
			var firstRequest = executor.submit(() -> {
				Thread.currentThread().setName("course-order-first-seat");
				return createSelf(first, lastSeat.runId(), "final-1").andReturn();
			});
			assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var secondRequest = executor.submit(() -> {
				Thread.currentThread().setName("course-order-second-seat");
				return createSelf(second, lastSeat.runId(), "final-2").andReturn();
			});
			assertThat(secondAtLock.await(10, TimeUnit.SECONDS)).isTrue();
			awaitDatabaseLock(secondPid.get(), firstPid.get());
			releaseFirst.countDown();

			List<MvcResult> results = List.of(firstRequest.get(10, TimeUnit.SECONDS),
				secondRequest.get(10, TimeUnit.SECONDS));
			assertThat(results).extracting(result -> result.getResponse().getStatus())
				.containsExactlyInAnyOrder(201, 409);
			assertThat(db.queryForObject("""
				select count(*) from course_reservations
				where course_run_id=? and status='HELD' and expires_at>?
				""", Long.class, lastSeat.runId(), Timestamp.from(NOW))).isOne();
		}
		finally {
			releaseFirst.countDown();
			executor.shutdownNow();
		}
	}

	private GymSessionWindow bookGymSession(MemberProfile member) {
		Account manager = account(AccountRole.MANAGER, AccountStatus.ACTIVE);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		UUID offerId = UUID.randomUUID(), orderId = UUID.randomUUID(), membershipId = UUID.randomUUID();
		UUID disciplineId = UUID.randomUUID(), classId = UUID.randomUUID(), roomId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		GymSessionWindow session = db.queryForObject("""
			select current_timestamp + interval '1 day' as start_at,
				current_timestamp + interval '1 day 1 hour' as end_at
			""", (result, row) -> new GymSessionWindow(
				result.getTimestamp("start_at").toInstant(), result.getTimestamp("end_at").toInstant()));
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,duration_days,status,
				created_by_account_id)
			values(?,'PLUS',?,'Course order overlap',12000,30,'ACTIVE',?)
			""", offerId, "Overlap " + offerId, manager.getId());
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
				duration_days_snapshot,payment_method,status,paid_at)
			values(?,?,?,?,?,'Overlap','PLUS',12000,'VND',30,'CASH','PAID',?)
			""", orderId, "ORD-" + orderId.toString().replace("-", "").toUpperCase(), member.getAccountId(),
			manager.getId(), offerId, Timestamp.from(session.startAt().minusSeconds(86400)));
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,provider_reference,paid_at,
				processed_by_account_id)
			values(?,?,'CASH','PAID',12000,'VND',?,?,?)
			""", UUID.randomUUID(), orderId, "CASH-" + orderId,
			Timestamp.from(session.startAt().minusSeconds(86400)), manager.getId());
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,
				price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at)
			values(?,?,?,?,'PLUS','Overlap',12000,'VND',30,'ACTIVE',?,?)
			""", membershipId, member.getAccountId(), orderId, offerId,
			Timestamp.from(session.startAt().minusSeconds(60)),
			Timestamp.from(session.endAt().plusSeconds(86400 * 30L)));
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", disciplineId,
			"Booked overlap " + disciplineId);
		db.update("insert into sport_classes(id,discipline_id,name,class_type,status) values(?,?,?,'GROUP','ACTIVE')",
			classId, disciplineId, "Booked overlap " + classId);
		db.update("insert into rooms(id,name,capacity,status) values(?,?,10,'ACTIVE')", roomId,
			"Booked overlap " + roomId);
		db.update("""
			insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,
				capacity,status,created_by_account_id)
			values(?,?,?,?,?,?,10,'SCHEDULED',?)
			""", sessionId, classId, coach.getId(), roomId, Timestamp.from(session.startAt()),
			Timestamp.from(session.endAt()), manager.getId());
		db.update("""
			insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id)
			values(?,?,?,?,'BOOKED',?)
			""", UUID.randomUUID(), sessionId, member.getAccountId(), membershipId, member.getAccountId());
		return session;
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout");
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private void awaitDatabaseLock(int waiterPid, int blockerPid) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			Long waiting = db.queryForObject("""
				select count(*) from pg_stat_activity
				where pid=? and wait_event_type='Lock' and ?=any(pg_blocking_pids(pid))
				""", Long.class, waiterPid, blockerPid);
			if (waiting != null && waiting == 1) return;
			Thread.onSpinWait();
		} while (System.nanoTime() < deadline);
		throw new AssertionError("Timed out waiting for the second CourseOrder transaction to block on CourseRun");
	}

	private org.springframework.test.web.servlet.ResultActions createSelf(MemberProfile member, UUID runId,
			String key) throws Exception {
		var request = post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + runId + "\"}");
		if (key != null) request.header("Idempotency-Key", key);
		return mockMvc.perform(request);
	}

	private org.springframework.test.web.servlet.ResultActions joinWaitlist(MemberProfile member, UUID runId)
			throws Exception {
		return mockMvc.perform(post("/api/v1/course-runs/{runId}/waitlist", runId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount())));
	}

	private org.springframework.test.web.servlet.ResultActions acceptWaitlist(MemberProfile member, UUID waitlistId,
			String key) throws Exception {
		return mockMvc.perform(post("/api/v1/course-waitlists/{waitlistId}/accept", waitlistId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount()))
			.header("Idempotency-Key", key));
	}

	private org.springframework.test.web.servlet.ResultActions createReception(Account receptionist,
			MemberProfile member, UUID runId, String key) throws Exception {
		return mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders", member.getAccountId())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + runId + "\"}"));
	}

	private org.springframework.test.web.servlet.ResultActions getSelf(MemberProfile member, UUID orderId)
			throws Exception {
		return mockMvc.perform(get("/api/v1/members/me/course-orders/{orderId}", orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount())));
	}

	private org.springframework.test.web.servlet.ResultActions getReception(Account receptionist,
			MemberProfile member, UUID orderId) throws Exception {
		return mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-orders/{orderId}",
			member.getAccountId(), orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)));
	}

	private List<MvcResult> concurrent(ThrowingRequest first, ThrowingRequest second) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(2);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var one = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return first.run(); });
			var two = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return second.run(); });
			return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
		}
	}

	private JsonNode json(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private long count(String table, UUID memberId) {
		return db.queryForObject("select count(*) from " + table + " where member_account_id=?", Long.class,
			memberId);
	}

	private MemberProfile member(AccountStatus status) {
		return profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER, status)));
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, status, role + " Course Order",
			"08" + id.toString().replace("-", "").substring(0, 8), id + "@course-order.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private RunFixture run(int capacity, Instant opensAt, Instant closesAt, String courseStatus,
			String runStatus, Instant sessionStart) {
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		UUID discipline = UUID.randomUUID();
		UUID course = UUID.randomUUID();
		UUID run = UUID.randomUUID();
		UUID room = UUID.randomUUID();
		UUID session = UUID.randomUUID();
		Instant plannedStart = closesAt.plusSeconds(3600);
		Instant plannedEnd = plannedStart.plusSeconds(86400);
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", discipline, "D-" + discipline);
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,?,'EXCLUSIVE','ACTIVE')",
			room, "Room " + room, capacity);
		db.update("insert into courses(id,code,name,discipline_id,status) values (?,?,?, ?,?)",
			course, "C-" + course, "Course " + course, discipline, courseStatus);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
				minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,
				planned_end_at,total_units,named_coach_commitment,policy_version_snapshot,status)
			values (?,?,?,?,500000,?,1,?,?,?,?,1,true,?,?)
			""", run, "R-" + run, course, coach.getId(), capacity, Timestamp.from(opensAt),
			Timestamp.from(closesAt), Timestamp.from(plannedStart), Timestamp.from(plannedEnd),
			"DRAFT".equals(runStatus) ? null : "P1", runStatus);
		db.update("""
			insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at,
				original_start_at,original_end_at,allocation_vnd,status)
			values (?,?,?,?,?,?,?,?,?,100000,'SCHEDULED')
			""", session, run, 1, room, coach.getId(), Timestamp.from(sessionStart),
			Timestamp.from(sessionStart.plusSeconds(3600)), Timestamp.from(sessionStart),
			Timestamp.from(sessionStart.plusSeconds(3600)));
		return new RunFixture(run, sessionStart);
	}

	private String token(Account account) {
		Instant issued = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(issued)
			.expiresAt(issued.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	@FunctionalInterface
	private interface ThrowingRequest {
		MvcResult run() throws Exception;
	}

	private record RunFixture(UUID runId, Instant sessionStart) { }

	private record GymSessionWindow(Instant startAt, Instant endAt) { }
}
