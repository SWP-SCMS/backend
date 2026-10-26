package com.scms.backend.identity.receptionmember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scms.backend.account.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ReceptionMemberMembershipSummaryIntegrationTests {

	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
	private Instant now;
	private LocalDate today;
	private static final AtomicInteger PHONES = new AtomicInteger(750_000_000);
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
	@Autowired ReceptionMemberProfileService service;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired MockMvc mvc;
	@Autowired JwtEncoder jwtEncoder;
	@MockitoSpyBean JdbcTemplate jdbc;
	@MockitoBean Clock clock;
	private Account receptionist;
	private MemberProfile member;
	private String plan;
	private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@BeforeEach
	void setUp() {
		today = jdbc.queryForObject("select (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date", LocalDate.class);
		now = today.atTime(12, 0).atZone(VIETNAM).toInstant();
		when(clock.instant()).thenReturn(now);
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		member = profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER, AccountStatus.ACTIVE), "photo", null));
		plan = "SUMMARY_" + UUID.randomUUID().toString().replace("-", "");
		jdbc.update("insert into membership_plans(plan_code,display_name,supports_booking,supports_personal_coaching,status) values(?,'Current plan',false,false,'INACTIVE')", plan);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void bothSearchCriteriaReturnActiveAlongsideReservedWithCurrentInactivePlan(boolean byPhone) throws Exception {
		UUID active = membership("ACTIVE", now.minusSeconds(82800), now.plusSeconds(2592000));
		membership("RESERVED", null, null);
		jdbc.update("insert into membership_plan_benefits(plan_code,benefit_code) values(?,'PERSONAL_COACHING'),(?,'CENTER_ACCESS'),(?,'CLASS_BOOKING')", plan, plan, plan);
		JsonNode summary = search(byPhone).path("activeMembership");
		assertThat(summary.path("membershipId").asText()).isEqualTo(active.toString());
		assertThat(summary.path("offerName").asText()).isEqualTo("Purchased offer");
		assertThat(summary.path("planName").asText()).isEqualTo("Current plan");
		assertThat(summary.path("effectiveStartDate").asText()).isEqualTo(today.minusDays(1).toString());
		assertThat(summary.path("effectiveEndDate").asText()).isEqualTo(today.plusDays(30).toString());
		assertThat(summary.path("benefitCodes").toString()).isEqualTo("[\"CENTER_ACCESS\",\"CLASS_BOOKING\",\"PERSONAL_COACHING\"]");
		assertThat(summary.path("status").asText()).isEqualTo("ACTIVE");
		assertThat(summary.path("frozen").asBoolean()).isFalse();
		mvc.perform(get("/api/v1/reception/members/search").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.param(byPhone ? "phone" : "memberId", byPhone ? member.getAccount().getPhone() : member.getMemberCode()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.activeMembership.membershipId").value(active.toString()));
	}

	@ParameterizedTest
	@CsvSource({"NONE,-1,1,false", "RESERVED,0,0,false", "CANCELLED,0,0,false", "EXPIRED,-2,-1,false", "TERMINATED,-1,1,false",
		"ACTIVE,0,1,true", "ACTIVE,-1,0,false", "ACTIVE,1,2,false"})
	void statusAndHalfOpenTimeBoundariesAreReadWithoutLifecycleMutation(String status, int start, int end, boolean current) {
		boolean undated = status.equals("RESERVED") || status.equals("CANCELLED");
		if (!status.equals("NONE")) membership(status, undated ? null : now.plusSeconds(start),
			undated ? null : now.plusSeconds(end));
		if (status.equals("TERMINATED") || status.equals("ACTIVE") && end == 0) membership("RESERVED", null, null);
		var before = jdbc.queryForList("select * from memberships where member_account_id=?", member.getAccountId());
		long audits = jdbc.queryForObject("select count(*) from audit_events", Long.class);
		long notifications = jdbc.queryForObject("select count(*) from notifications", Long.class);
		JsonNode response = search(false);
		assertThat(response.has("activeMembership")).isTrue();
		assertThat(response.path("activeMembership").isNull()).isEqualTo(!current);
		assertThat(jdbc.queryForList("select * from memberships where member_account_id=?", member.getAccountId())).isEqualTo(before);
		assertThat(jdbc.queryForObject("select count(*) from audit_events", Long.class)).isEqualTo(audits);
		assertThat(jdbc.queryForObject("select count(*) from notifications", Long.class)).isEqualTo(notifications);
	}

	@ParameterizedTest
	@CsvSource({"SCHEDULED,1,3,false", "SCHEDULED,-1,1,false", "ACTIVE,0,1,true", "ACTIVE,-1,0,false",
		"ACTIVE,1,2,false", "COMPLETED,-1,1,false", "ENDED_EARLY,-1,1,false"})
	void frozenUsesOnlyEffectiveActivePeriodAndKeepsMembershipActive(String periodStatus, int start, int end, boolean frozen) {
		UUID active = membership("ACTIVE", now.minusSeconds(86400), now.plusSeconds(864000));
		freeze(active, periodStatus, today.plusDays(start), today.plusDays(end), null);
		var periods = jdbc.queryForList("select * from membership_freeze_periods where membership_id=?", active);
		JsonNode summary = search(false).path("activeMembership");
		assertThat(summary.path("membershipId").asText()).isEqualTo(active.toString());
		assertThat(summary.path("status").asText()).isEqualTo("ACTIVE");
		assertThat(summary.path("frozen").asBoolean()).isEqualTo(frozen);
		assertThat(jdbc.queryForList("select * from membership_freeze_periods where membership_id=?", active)).isEqualTo(periods);
	}

	@Test
	void activePeriodWithActualEndTodayIsNotFrozenAndOtherMembershipFreezeDoesNotLeak() {
		UUID active = membership("ACTIVE", now.minusSeconds(86400), now.plusSeconds(864000));
		freeze(active, "ACTIVE", today.minusDays(1), today.plusDays(2), today);
		assertThat(search(false).at("/activeMembership/frozen").asBoolean()).isFalse();
		UUID reserved = membership("RESERVED", null, null);
		freeze(reserved, "ACTIVE", today.minusDays(1), today.plusDays(2), null);
		assertThat(search(false).at("/activeMembership/frozen").asBoolean()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 3})
	void membershipPlanBenefitsAndFreezeUseOneQueryRegardlessOfBenefits(int benefitCount) {
		membership("ACTIVE", now.minusSeconds(1), now.plusSeconds(86400));
		String[] codes = {"PERSONAL_COACHING", "CENTER_ACCESS", "CLASS_BOOKING"};
		for (int i = 0; i < benefitCount; i++) jdbc.update("insert into membership_plan_benefits values(?,?)", plan, codes[i]);
		clearInvocations(jdbc);
		assertThat(search(false).at("/activeMembership/benefitCodes").size()).isEqualTo(benefitCount);
		verify(jdbc).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"MEMBER", "COACH", "MANAGER"})
	void otherActiveRolesCannotSearch(AccountRole role) throws Exception {
		mvc.perform(get("/api/v1/reception/members/search").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(account(role, AccountStatus.ACTIVE)))
			.param("memberId", member.getMemberCode())).andExpect(status().isForbidden());
	}

	@Test
	void inactiveReceptionistAndAnonymousCannotSearchAndCriteriaRemainExclusive() throws Exception {
		mvc.perform(get("/api/v1/reception/members/search").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(account(AccountRole.RECEPTIONIST, AccountStatus.INACTIVE)))
			.param("memberId", member.getMemberCode())).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/reception/members/search").contextPath("/api/v1")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/reception/members/search").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.param("memberId", member.getMemberCode()).param("phone", member.getAccount().getPhone()))
			.andExpect(status().isBadRequest());
	}

	private JsonNode search(boolean byPhone) {
		return json.valueToTree(service.search(receptionist.getId(), byPhone ? null : member.getMemberCode(),
			byPhone ? member.getAccount().getPhone() : null));
	}

	private UUID membership(String status, Instant start, Instant end) {
		UUID offer = UUID.randomUUID(), order = UUID.randomUUID(), id = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,?,'Current offer','Summary fixture',100000,'VND',30,'ACTIVE',?)", offer, plan, receptionist.getId());
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at,fulfillment_mode) values(?,?,?,?,?,'Purchased offer',?,100000,'VND',30,'CASH','PAID',?,?)",
			order, "SUM-" + order, member.getAccountId(), receptionist.getId(), offer, plan, Timestamp.from(now), start == null ? "RESERVED" : "IMMEDIATE");
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at) values(?,?,'CASH','PAID',100000,'VND',?,?)", UUID.randomUUID(), order, receptionist.getId(), Timestamp.from(now));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Purchased offer',100000,'VND',30,?,?,?)",
			id, member.getAccountId(), order, offer, plan, status, start == null ? null : Timestamp.from(start), end == null ? null : Timestamp.from(end));
		return id;
	}

	private void freeze(UUID membership, String status, LocalDate start, LocalDate end, LocalDate actualEnd) {
		UUID request = UUID.randomUUID();
		jdbc.update("insert into membership_freeze_requests(id,membership_id,member_account_id,requester_account_id,start_date,end_date,reason,status) values(?,?,?,?,?,?,'Summary fixture','COMPLETED')",
			request, membership, member.getAccountId(), member.getAccountId(), java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));
		jdbc.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,actual_start_date,actual_end_date,status) values(?,?,?,?,?,?,?,?)",
			UUID.randomUUID(), request, membership, java.sql.Date.valueOf(start), java.sql.Date.valueOf(end), status.equals("SCHEDULED") ? null : java.sql.Date.valueOf(start), actualEnd == null ? null : java.sql.Date.valueOf(actualEnd), status);
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, status, "Summary " + role, "0" + PHONES.getAndIncrement(),
			id + "@summary.test", LocalDate.of(1990, 1, 1), "hash"));
	}

	private String token(Account account) {
		Instant now = Instant.now();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
			JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(3600))
				.claim("role", account.getRole().name()).build())).getTokenValue();
	}
}
