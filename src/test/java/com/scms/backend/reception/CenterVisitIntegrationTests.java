package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
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

	@Test
	void receptionistCreatesSeparateVisitsByMemberCodeOrPhone() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = member(AccountStatus.ACTIVE, "https://example.test/member.jpg");
		UUID membershipId = membership(member.getAccountId(), receptionist.getId());

		checkIn(receptionist, "{\"memberId\":\"" + member.getMemberCode() + "\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(member.getAccountId().toString()))
			.andExpect(jsonPath("$.memberCode").value(member.getMemberCode()))
			.andExpect(jsonPath("$.membershipId").value(membershipId.toString()))
			.andExpect(jsonPath("$.checkedInBy").value(receptionist.getId().toString()));
		checkIn(receptionist, "{\"phone\":\"" + member.getAccount().getPhone() + "\"}")
			.andExpect(status().isCreated());

		assertThat(jdbc.queryForObject("select count(*) from center_visits where member_account_id = ?",
			Long.class, member.getAccountId())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select count(distinct id) from center_visits where member_account_id = ?",
			Long.class, member.getAccountId())).isEqualTo(2L);
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
		return mockMvc.perform(post("/api/v1/reception/center-visits").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(actor))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
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
