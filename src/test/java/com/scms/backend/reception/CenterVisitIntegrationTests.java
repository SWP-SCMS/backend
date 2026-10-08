package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CenterVisitIntegrationTests {
	private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(36_000_000);

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mockMvc;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate jdbc;
	@Autowired JwtEncoder jwtEncoder;
	@Autowired CenterVisitRepository visits;

	@Test
	void identityVerificationIsRequired() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = member(AccountStatus.ACTIVE, "https://example.test/member.jpg");
		membership(member.getAccountId(), receptionist.getId());

		checkIn(receptionist, "{\"memberId\":\"" + member.getMemberCode() + "\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("IDENTITY_VERIFICATION_REQUIRED"));
		checkIn(receptionist, "{\"memberId\":\"" + member.getMemberCode()
			+ "\",\"identityVerified\":false}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("IDENTITY_VERIFICATION_REQUIRED"));
	}

	@Test
	void receptionistCreatesSeparateVisitsByMemberCodeOrPhone() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = member(AccountStatus.ACTIVE, "https://example.test/member.jpg");
		UUID membershipId = membership(member.getAccountId(), receptionist.getId());

		checkIn(receptionist, "{\"memberId\":\"" + member.getMemberCode() + "\",\"identityVerified\":true}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(member.getAccountId().toString()))
			.andExpect(jsonPath("$.memberCode").value(member.getMemberCode()))
			.andExpect(jsonPath("$.membershipId").value(membershipId.toString()))
			.andExpect(jsonPath("$.checkedInBy").value(receptionist.getId().toString()));
		checkIn(receptionist, "{\"phone\":\"" + member.getAccount().getPhone() + "\",\"identityVerified\":true}")
			.andExpect(status().isCreated());

		assertThat(jdbc.queryForObject("select count(*) from center_visits where member_account_id = ?",
			Long.class, member.getAccountId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select count(distinct id) from center_visits where member_account_id = ?",
			Long.class, member.getAccountId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select count(*) from center_visits where member_account_id=? and checked_out_at is null",
			Long.class, member.getAccountId())).isOne();
	}

	@Test
	void idempotencyReplaysAndRejectsReuseForAnotherMember() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile first = member(AccountStatus.ACTIVE, "photo");
		MemberProfile second = member(AccountStatus.ACTIVE, "photo");
		membership(first.getAccountId(), receptionist.getId());
		membership(second.getAccountId(), receptionist.getId());

		checkIn(receptionist, request(first), "visit-key")
			.andExpect(status().isCreated()).andExpect(jsonPath("$.created").value(true));
		Instant checkedInAt = jdbc.queryForObject("select checked_in_at from center_visits where member_account_id=?",
			Instant.class, first.getAccountId());
		jdbc.update("update accounts set status='SUSPENDED' where id=?", first.getAccountId());
		jdbc.update("update member_profiles set profile_image_url=null where account_id=?", first.getAccountId());
		checkIn(receptionist, request(first), " visit-key ")
			.andExpect(status().isOk()).andExpect(jsonPath("$.created").value(false));
		assertThat(jdbc.queryForObject("select checked_in_at from center_visits where member_account_id=?",
			Instant.class, first.getAccountId())).isEqualTo(checkedInAt);
		assertThat(jdbc.queryForObject("select count(*) from center_visits where member_account_id=?",
			Long.class, first.getAccountId())).isOne();
		checkIn(receptionist, request(second), "visit-key")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
	}

	@Test
	void concurrentSameKeyCreatesOneVisit() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = member(AccountStatus.ACTIVE, "photo");
		membership(member.getAccountId(), receptionist.getId());
		String token = token(receptionist);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> request = () -> {
			ready.countDown();
			start.await();
			return mockMvc.perform(post("/api/v1/reception/center-visits").contextPath("/api/v1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header("Idempotency-Key", "concurrent-key")
				.contentType(MediaType.APPLICATION_JSON).content(request(member)))
				.andReturn().getResponse().getStatus();
		};
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(request);
			var second = executor.submit(request);
			ready.await();
			start.countDown();
			assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 201);
		}
		assertThat(jdbc.queryForObject("select count(*) from center_visits where member_account_id=?",
			Long.class, member.getAccountId())).isOne();
	}

	@Test
	void memberAndReceptionistCheckoutReplayOriginalTimestampAndRespectOwnership() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile owner = member(AccountStatus.ACTIVE, "photo");
		MemberProfile other = member(AccountStatus.ACTIVE, "photo");
		membership(owner.getAccountId(), receptionist.getId());
		checkIn(receptionist, request(owner), null).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/members/me/center-visits/current").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(owner.getAccount())))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/v1/members/me/center-visits/current/checkout").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(other.getAccount())))
			.andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/v1/members/me/center-visits/current/checkout").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(owner.getAccount())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.checkoutSource").value("MEMBER"));
		Instant checkout = jdbc.queryForObject("select checked_out_at from center_visits where member_account_id=?",
			Instant.class, owner.getAccountId());
		mockMvc.perform(patch("/api/v1/members/me/center-visits/current/checkout").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(owner.getAccount())))
			.andExpect(status().isOk());
		assertThat(jdbc.queryForObject("select checked_out_at from center_visits where member_account_id=?",
			Instant.class, owner.getAccountId())).isEqualTo(checkout);

		checkIn(receptionist, request(owner), "reentry").andExpect(status().isCreated());
		mockMvc.perform(patch("/api/v1/reception/members/{id}/center-visits/current/checkout", owner.getAccountId())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.checkoutSource").value("RECEPTIONIST"));
		Instant receptionistCheckout = jdbc.queryForObject("""
			select checked_out_at from center_visits where member_account_id=?
			order by checked_in_at desc, id desc limit 1
			""", Instant.class, owner.getAccountId());
		mockMvc.perform(patch("/api/v1/reception/members/{id}/center-visits/current/checkout", owner.getAccountId())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.checkoutSource").value("RECEPTIONIST"));
		assertThat(jdbc.queryForObject("""
			select checked_out_at from center_visits where member_account_id=?
			order by checked_in_at desc, id desc limit 1
			""", Instant.class, owner.getAccountId())).isEqualTo(receptionistCheckout);
	}

	@Test
	void openVisitsArePaginatedAndOverlapUsesHalfOpenIntervals() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		jdbc.update("""
			update center_visits
			set checked_out_at=greatest(checked_in_at,current_timestamp), checked_out_by_account_id=?,
				checkout_source='RECEPTIONIST'
			where checked_out_at is null
			""", receptionist.getId());
		MemberProfile first = member(AccountStatus.ACTIVE, "photo");
		MemberProfile second = member(AccountStatus.ACTIVE, "photo");
		MemberProfile third = member(AccountStatus.ACTIVE, "photo");
		UUID firstMembership = membership(first.getAccountId(), receptionist.getId());
		membership(second.getAccountId(), receptionist.getId());
		membership(third.getAccountId(), receptionist.getId());
		checkIn(receptionist, request(first), null).andExpect(status().isCreated());
		checkIn(receptionist, request(second), null).andExpect(status().isCreated());
		checkIn(receptionist, request(third), null).andExpect(status().isCreated());
		Instant firstTime = Instant.parse("2026-10-06T01:00:00Z");
		Instant secondTime = Instant.parse("2026-10-06T02:00:00Z");
		Instant thirdTime = Instant.parse("2026-10-06T03:00:00Z");
		jdbc.update("update center_visits set checked_in_at=? where member_account_id=?",
			java.sql.Timestamp.from(firstTime), first.getAccountId());
		jdbc.update("update center_visits set checked_in_at=? where member_account_id=?",
			java.sql.Timestamp.from(secondTime), second.getAccountId());
		jdbc.update("update center_visits set checked_in_at=? where member_account_id=?",
			java.sql.Timestamp.from(thirdTime), third.getAccountId());
		jdbc.update("""
			update center_visits set checked_out_at=?, checked_out_by_account_id=?, checkout_source='RECEPTIONIST'
			where member_account_id=?
			""", java.sql.Timestamp.from(secondTime.plusSeconds(60)), receptionist.getId(), second.getAccountId());
		UUID firstVisit = jdbc.queryForObject("select id from center_visits where member_account_id=?",
			UUID.class, first.getAccountId());
		UUID thirdVisit = jdbc.queryForObject("select id from center_visits where member_account_id=?",
			UUID.class, third.getAccountId());

		mockMvc.perform(get("/api/v1/reception/center-visits/open?page=0&size=1").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(firstVisit.toString()))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(2));
		mockMvc.perform(get("/api/v1/reception/center-visits/open?page=1&size=1").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(thirdVisit.toString()))
			.andExpect(jsonPath("$.totalElements").value(2));

		jdbc.update("update center_visits set checked_in_at=?,checked_out_at=?,checked_out_by_account_id=?,checkout_source='RECEPTIONIST' where member_account_id=?",
			java.sql.Timestamp.from(Instant.parse("2026-10-05T01:00:00Z")),
			java.sql.Timestamp.from(Instant.parse("2026-10-05T02:00:00Z")), receptionist.getId(), first.getAccountId());
		assertThat(visits.findOverlappingVisitId(first.getAccountId(), Instant.parse("2026-10-05T10:00:00Z"),
			Instant.parse("2026-10-05T11:00:00Z"), Instant.now())).isEmpty();
		UUID overlap = UUID.randomUUID();
		jdbc.update("insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at,checked_out_at,checked_out_by_account_id,checkout_source) values(?,?,?,?,?,?,?,'RECEPTIONIST')",
			overlap, first.getAccountId(), firstMembership, receptionist.getId(),
			java.sql.Timestamp.from(Instant.parse("2026-10-05T09:30:00Z")),
			java.sql.Timestamp.from(Instant.parse("2026-10-05T10:30:00Z")), receptionist.getId());
		assertThat(visits.findOverlappingVisitId(first.getAccountId(), Instant.parse("2026-10-05T10:00:00Z"),
			Instant.parse("2026-10-05T11:00:00Z"), Instant.now())).contains(overlap);
	}

	@Test
	void coachCannotUseReceptionCheckInEndpoint() throws Exception {
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		mockMvc.perform(post("/api/v1/reception/center-visits").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(coach))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"memberId\":\"MB-100001\"}"))
			.andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.ResultActions checkIn(Account actor, String body) throws Exception {
		return checkIn(actor, body, null);
	}

	private org.springframework.test.web.servlet.ResultActions checkIn(Account actor, String body, String key)
			throws Exception {
		var request = post("/api/v1/reception/center-visits").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(actor))
			.contentType(MediaType.APPLICATION_JSON).content(body);
		if (key != null) request.header("Idempotency-Key", key);
		return mockMvc.perform(request);
	}

	private String request(MemberProfile member) {
		return "{\"memberId\":\"" + member.getMemberCode() + "\",\"identityVerified\":true}";
	}

	private MemberProfile member(AccountStatus status, String image) {
		return profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER, status), image, null));
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, status, role + " US36",
			"09" + PHONE_SEQUENCE.getAndIncrement(), id + "@us36.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private UUID membership(UUID memberId, UUID actorId) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, 'BASIC', 'Basic US36', 'Center access', 12000, 30, 'ACTIVE', ?)
			""", offerId, actorId);
		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Basic US36', 'BASIC', 12000, 'VND', 30, 'CASH', 'PAID', ?, ?)
			""", orderId, "ORD-" + orderId, memberId, actorId, offerId,
			java.sql.Timestamp.from(Instant.now().plusSeconds(3600)), java.sql.Timestamp.from(Instant.now()));
		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 12000, 'VND', ?, ?)
			""", paymentId, orderId, actorId, java.sql.Timestamp.from(Instant.now()));
		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, 'BASIC', 'Basic US36', 12000, 'VND', 30, 'ACTIVE', ?, ?)
			""", membershipId, memberId, orderId, offerId,
			java.sql.Timestamp.from(Instant.now().minusSeconds(60)),
			java.sql.Timestamp.from(Instant.now().plusSeconds(3600)));
		return membershipId;
	}

	private String token(Account account) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now)
			.expiresAt(now.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}
}
