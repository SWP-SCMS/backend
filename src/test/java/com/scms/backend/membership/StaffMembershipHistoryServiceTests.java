package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class StaffMembershipHistoryServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final PaymentFulfillmentService fulfillment = mock(PaymentFulfillmentService.class);
	private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private PaymentService service;

	@BeforeEach
	void setUp() {
		service = new PaymentService(db, accounts, audits, fulfillment, profiles, memberships,
			Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = { "ACTIVE", "SUSPENDED" })
	void receptionistCanReadEmptyHistoryForVisibleMember(AccountStatus targetStatus) {
		UUID actor = UUID.randomUUID();
		Account member = account(AccountRole.MEMBER, targetStatus);
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(profiles.findVisibleMemberByCode("MB-100001", AccountRole.MEMBER,
			List.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED)))
			.thenReturn(Optional.of(new MemberProfile(member)));
		when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

		assertThat(service.receptionMembershipHistory(actor, "MB-100001")).isEmpty();
		verify(memberships).expireDueForMember(member.getId(), NOW);
		verify(memberships, never()).expireDue(any());
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = { "ACTIVE", "SUSPENDED" })
	void managerCanReadEmptyHistoryForMemberAccount(AccountStatus targetStatus) {
		UUID actor = UUID.randomUUID();
		Account member = account(AccountRole.MEMBER, targetStatus);
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accounts.findById(member.getId())).thenReturn(Optional.of(member));
		when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

		assertThat(service.managerMembershipHistory(actor, member.getId())).isEmpty();
		verify(memberships).expireDueForMember(member.getId(), NOW);
		verify(memberships, never()).expireDue(any());
	}

	@Test
	void inactiveAndCrossRoleStaffAreRejectedBeforeTargetLookup() {
		UUID actor = UUID.randomUUID();

		assertThatThrownBy(() -> service.receptionMembershipHistory(actor, "MB-100001"))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		assertThatThrownBy(() -> service.managerMembershipHistory(actor, UUID.randomUUID()))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		verify(profiles, never()).findVisibleMemberByCode(anyString(), any(), any());
		verify(accounts, never()).findById(any());
		verify(memberships, never()).expireDue(any());
		verify(memberships, never()).expireDueForMember(any(), any());
	}

	@Test
	void missingAndNonMemberTargetsUsePaymentNotFoundContract() {
		UUID receptionist = UUID.randomUUID();
		UUID manager = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);

		assertPaymentNotFound(() -> service.receptionMembershipHistory(receptionist, "MB-MISSING"));
		assertPaymentNotFound(() -> service.managerMembershipHistory(manager, UUID.randomUUID()));
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		assertPaymentNotFound(() -> service.managerMembershipHistory(manager, coach.getId()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void reconcilesBeforeReturningOwnedSnapshotHistoryInDeterministicOrderAndUtc() throws Exception {
		UUID manager = UUID.randomUUID();
		Account member = account(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID firstId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
		UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		Instant startsAt = Instant.parse("2026-10-01T01:02:03Z");
		Instant firstEnd = Instant.parse("2026-11-01T01:02:03Z");
		Instant secondEnd = Instant.parse("2026-10-18T11:59:59Z");
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accounts.findById(member.getId())).thenReturn(Optional.of(member));
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<RowMapper<StaffMembershipResponse>> mapper = ArgumentCaptor.forClass(RowMapper.class);
		when(db.query(sql.capture(), mapper.capture(), any(Object[].class))).thenAnswer(invocation -> {
			String query = invocation.getArgument(0);
			if (query.contains("membership_plan_benefits")) {
				RowMapper<Object> benefitMapper = invocation.getArgument(1);
				ResultSet rs = mock(ResultSet.class);
				when(rs.getString("plan_code")).thenReturn("PLUS");
				when(rs.getString("code")).thenReturn("CLASS_BOOKING");
				when(rs.getString("display_name")).thenReturn("Class booking");
				when(rs.getString("description")).thenReturn("Book classes");
				when(rs.getString("status")).thenReturn("ACTIVE");
				return List.of(benefitMapper.mapRow(rs, 0));
			}
			RowMapper<StaffMembershipResponse> historyMapper = invocation.getArgument(1);
			return List.of(
				map(historyMapper, firstId, "PLUS", "Snapshot Plus", "ACTIVE", startsAt, firstEnd),
				map(historyMapper, secondId, "BASIC", "Snapshot Basic", "EXPIRED", startsAt, secondEnd));
		});

		List<StaffMembershipResponse> result = service.managerMembershipHistory(manager, member.getId());

		assertThat(result).containsExactly(
			new StaffMembershipResponse(firstId, "PLUS", "Snapshot Plus", "ACTIVE", startsAt, firstEnd,
				List.of("CLASS_BOOKING"), List.of(new MembershipBenefitResponse("CLASS_BOOKING", "Class booking",
					"Book classes", MembershipBenefitStatus.ACTIVE))),
			new StaffMembershipResponse(secondId, "BASIC", "Snapshot Basic", "EXPIRED", startsAt, secondEnd));
		assertThat(sql.getAllValues().getFirst()).contains("where member_account_id=?", "order by starts_at desc,id desc");
		assertThat(sql.getAllValues().get(1)).contains("membership_plan_benefits", "where link.plan_code in (?,?)");
		InOrder order = inOrder(memberships, db);
		order.verify(memberships).expireDueForMember(member.getId(), NOW);
		order.verify(db).query(eq(sql.getAllValues().getFirst()), any(RowMapper.class), any(Object[].class));
		order.verify(db).query(eq(sql.getAllValues().get(1)), any(RowMapper.class), any(Object[].class));
		verify(memberships, never()).expireDue(any());
	}

	private StaffMembershipResponse map(RowMapper<StaffMembershipResponse> mapper, UUID id, String plan,
			String offer, String status, Instant startsAt, Instant endsAt) throws Exception {
		ResultSet rs = mock(ResultSet.class);
		when(rs.getObject("id", UUID.class)).thenReturn(id);
		when(rs.getString("plan_code_snapshot")).thenReturn(plan);
		when(rs.getString("offer_name_snapshot")).thenReturn(offer);
		when(rs.getString("status")).thenReturn(status);
		when(rs.getTimestamp("starts_at")).thenReturn(Timestamp.from(startsAt));
		when(rs.getTimestamp("ends_at")).thenReturn(Timestamp.from(endsAt));
		return mapper.mapRow(rs, 0);
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return new Account(id, role, status, role + " Test", "0900000000", id + "@example.test",
			LocalDate.of(1990, 1, 1), "encoded-password");
	}

	private void assertPaymentNotFound(Runnable operation) {
		assertThatThrownBy(operation::run).isInstanceOfSatisfying(PaymentException.class, exception -> {
			assertThat(exception.status().value()).isEqualTo(404);
			assertThat(exception.code()).isEqualTo("PAYMENT_NOT_FOUND");
		});
	}
}
