package com.scms.backend.identity.receptionmember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.account.*;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.identity.memberprofile.MemberProfileDetailsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
class ReceptionMemberSearchServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-24T17:00:00Z");
	private static final Set<AccountStatus> VISIBLE = Set.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
	@Mock AccountRepository accounts;
	@Mock MemberProfileRepository profiles;
	@Mock AccountIdentifierAvailability availability;
	@Mock JdbcTemplate db;
	@Mock Clock clock;
	@Mock ResultSet rows;
	@Spy MemberProfileDetailsValidator validator = new MemberProfileDetailsValidator(Clock.fixed(NOW, ZoneOffset.UTC));
	@InjectMocks ReceptionMemberProfileService service;
	private final UUID actor = UUID.randomUUID();
	private final UUID member = UUID.randomUUID();
	private final UUID membership = UUID.randomUUID();
	private MemberProfile profile;
	private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
		.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@BeforeEach
	void setUp() {
		profile = new MemberProfile(new Account(member, AccountRole.MEMBER, AccountStatus.ACTIVE,
			"Member", "0901234567", "member@example.test", LocalDate.of(1990, 1, 1), "hash"), "photo", null);
		lenient().when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(profiles.findVisibleMemberByCode("MB-123", AccountRole.MEMBER, VISIBLE))
			.thenReturn(Optional.of(profile));
		lenient().when(profiles.findVisibleMemberByPhone("0901234567", AccountRole.MEMBER, VISIBLE))
			.thenReturn(Optional.of(profile));
		lenient().when(db.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
			.thenAnswer(call -> ((ResultSetExtractor<?>) call.getArgument(1)).extractData(rows));
	}

	@ParameterizedTest
	@CsvSource({"false,true", "true,true", "false,false"})
	void bothCriteriaReturnSnapshotAndVietnamDatesWithSortedDistinctBenefits(boolean byPhone, boolean resolvedPlan) throws Exception {
		if (resolvedPlan) when(rows.next()).thenReturn(true, true, true, false);
		else when(rows.next()).thenReturn(true, false);
		when(rows.getObject("id", UUID.class)).thenReturn(membership);
		when(rows.getString("offer_name_snapshot")).thenReturn("Purchased offer");
		when(rows.getString("plan_name")).thenReturn(resolvedPlan ? "Current inactive plan name" : null);
		if (!resolvedPlan) when(rows.getString("plan_code_snapshot")).thenReturn("LEGACY_MISSING");
		when(rows.getString("status")).thenReturn("ACTIVE");
		when(rows.getTimestamp("starts_at")).thenReturn(Timestamp.from(Instant.parse("2026-10-23T18:00:00Z")));
		when(rows.getTimestamp("ends_at")).thenReturn(Timestamp.from(Instant.parse("2026-11-23T18:00:00Z")));
		when(rows.getBoolean("frozen")).thenReturn(true);
		if (resolvedPlan) when(rows.getString("benefit_code")).thenReturn("PERSONAL_COACHING", "CENTER_ACCESS", "CENTER_ACCESS");

		JsonNode response = search(byPhone);

		assertThat(response.path("accountId").asText()).isEqualTo(member.toString());
		assertThat(response.path("phone").asText()).isEqualTo("0901234567");
		JsonNode summary = response.path("activeMembership");
		assertThat(summary.path("membershipId").asText()).isEqualTo(membership.toString());
		assertThat(summary.path("offerName").asText()).isEqualTo("Purchased offer");
		assertThat(summary.path("planName").asText()).isEqualTo(resolvedPlan ? "Current inactive plan name" : "LEGACY_MISSING");
		assertThat(summary.path("effectiveStartDate").asText()).isEqualTo("2026-10-24");
		assertThat(summary.path("effectiveEndDate").asText()).isEqualTo("2026-11-24");
		assertThat(summary.path("status").asText()).isEqualTo("ACTIVE");
		assertThat(summary.path("frozen").asBoolean()).isTrue();
		assertThat(summary.path("benefitCodes").toString()).isEqualTo(resolvedPlan ? "[\"CENTER_ACCESS\",\"PERSONAL_COACHING\"]" : "[]");
		verify(db).query(anyString(), any(ResultSetExtractor.class), eq(member), eq(Timestamp.from(NOW)),
			eq(Timestamp.from(NOW)), eq(java.sql.Date.valueOf("2026-10-25")), eq(java.sql.Date.valueOf("2026-10-25")));
		verifyNoMoreInteractions(db);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void noCurrentPackageIsExplicitNullWithOneRead(boolean byPhone) throws Exception {
		when(rows.next()).thenReturn(false);
		JsonNode response = search(byPhone);
		assertThat(response.has("activeMembership")).isTrue();
		assertThat(response.path("activeMembership").isNull()).isTrue();
		verify(db).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));
		verifyNoMoreInteractions(db);
	}

	@Test
	void invalidCriteriaDoNotReadMemberships() {
		assertThatThrownBy(() -> service.search(actor, null, null)).isInstanceOf(ReceptionMemberValidationException.class);
		assertThatThrownBy(() -> service.search(actor, "MB-123", "0901234567"))
			.isInstanceOf(ReceptionMemberValidationException.class);
		assertThatThrownBy(() -> service.search(actor, null, "+84901234567"))
			.isInstanceOf(com.scms.backend.identity.memberprofile.MemberProfileValidationException.class);
		verifyNoInteractions(db);
	}

	@Test
	void inactiveOrWrongRoleActorCannotReadSummary() {
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(false);
		assertThatThrownBy(() -> service.search(actor, "MB-123", null)).isInstanceOf(InvalidAuthenticatedAccountException.class);
		verifyNoInteractions(profiles, db);
	}

	private JsonNode search(boolean byPhone) {
		return json.valueToTree(service.search(actor, byPhone ? null : " MB-123 ", byPhone ? " 0901234567 " : null));
	}
}
