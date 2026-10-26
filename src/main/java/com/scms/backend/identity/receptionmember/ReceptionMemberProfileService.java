package com.scms.backend.identity.receptionmember;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.identity.memberprofile.MemberProfileDetailsValidator;
import com.scms.backend.identity.memberprofile.MemberProfilePatchRequest;
import com.scms.backend.identity.memberprofile.MemberProfileResponse;
import com.scms.backend.membership.MembershipStatus;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceptionMemberProfileService {

	private static final Set<AccountStatus> VISIBLE_MEMBER_STATUSES =
		Set.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final MemberProfileDetailsValidator validator;
	private final JdbcTemplate db;
	private final Clock clock;

	ReceptionMemberProfileService(AccountRepository accountRepository,
			MemberProfileRepository memberProfileRepository,
			AccountIdentifierAvailability identifierAvailability,
			MemberProfileDetailsValidator validator, JdbcTemplate db, Clock clock) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.identifierAvailability = identifierAvailability;
		this.validator = validator;
		this.db = db;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ReceptionMemberSearchResponse search(UUID receptionistAccountId, String memberId, String phone) {
		ensureActiveReceptionist(receptionistAccountId);
		if ((memberId == null) == (phone == null)) {
			throw new ReceptionMemberValidationException("criteria",
				"exactly one of memberId or phone must be provided");
		}

		MemberProfile profile;
		if (memberId != null) {
			String normalizedMemberId = validator.normalizeRequired(memberId, "memberId", 20);
			profile = findByMemberId(normalizedMemberId);
		}
		else {
			String normalizedPhone = validator.normalizePhone(phone, "phone");
			profile = memberProfileRepository.findVisibleMemberByPhone(normalizedPhone, AccountRole.MEMBER,
				VISIBLE_MEMBER_STATUSES).orElseThrow(ReceptionMemberNotFoundException::new);
		}
		return toSearchResponse(profile);
	}

	@Transactional(readOnly = true)
	public MemberProfileResponse getProfile(UUID receptionistAccountId, String memberId) {
		ensureActiveReceptionist(receptionistAccountId);
		return toProfileResponse(findByMemberId(normalizeMemberId(memberId)));
	}

	@Transactional
	MemberProfileResponse updateProfile(UUID receptionistAccountId, String memberId,
			MemberProfilePatchRequest request) {
		ensureActiveReceptionist(receptionistAccountId);
		MemberProfile profile = findByMemberId(normalizeMemberId(memberId));
		Account account = profile.getAccount();

		String fullName = request.hasFullName()
			? validator.normalizeRequired(request.fullName(), "fullName", 200)
			: account.getFullName();
		String phone = request.hasPhone()
			? validator.normalizePhone(request.phone(), "phone")
			: account.getPhone();
		String email = request.hasEmail()
			? validator.normalizeEmail(request.email())
			: account.getEmail();
		LocalDate birthDate = request.hasBirthDate()
			? validator.validateBirthDate(request.birthDate())
			: account.getBirthDate();
		String profileImageUrl = request.hasProfileImageUrl()
			? validator.normalizeOptional(request.profileImageUrl(), "profileImageUrl", null)
			: profile.getProfileImageUrl();
		String fitnessGoal = request.hasFitnessGoal()
			? validator.normalizeOptional(request.fitnessGoal(), "fitnessGoal", null)
			: profile.getFitnessGoal();
		String emergencyContactName = request.hasEmergencyContactName()
			? validator.normalizeOptional(request.emergencyContactName(), "emergencyContactName", 200)
			: profile.getEmergencyContactName();
		String emergencyContactPhone = request.hasEmergencyContactPhone()
			? validator.normalizeOptionalPhone(request.emergencyContactPhone())
			: profile.getEmergencyContactPhone();

		validator.validateEmergencyContact(emergencyContactName, emergencyContactPhone);
		identifierAvailability.ensureAvailableForUpdate(account.getId(), account.getEmail(), account.getPhone(),
			email, phone);

		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		profile.updateDetails(profileImageUrl, fitnessGoal, emergencyContactName, emergencyContactPhone);
		try {
			accountRepository.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}
		return toProfileResponse(profile);
	}

	@Transactional
	public String replaceProfileImage(UUID receptionistAccountId, String memberId, String profileImageUrl) {
		ensureActiveReceptionist(receptionistAccountId);
		MemberProfile profile = findByMemberId(normalizeMemberId(memberId));
		String oldProfileImageUrl = profile.getProfileImageUrl();
		profile.updateDetails(profileImageUrl, profile.getFitnessGoal(), profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone());
		memberProfileRepository.flush();
		return oldProfileImageUrl;
	}

	private String normalizeMemberId(String memberId) {
		if (memberId == null || memberId.trim().isEmpty()) {
			throw new ReceptionMemberNotFoundException();
		}
		return memberId.trim();
	}

	private void ensureActiveReceptionist(UUID accountId) {
		if (!accountRepository.existsByIdAndRoleAndStatus(accountId, AccountRole.RECEPTIONIST,
				AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private MemberProfile findByMemberId(String memberId) {
		return memberProfileRepository.findVisibleMemberByCode(memberId, AccountRole.MEMBER,
			VISIBLE_MEMBER_STATUSES).orElseThrow(ReceptionMemberNotFoundException::new);
	}

	private ReceptionMemberSearchResponse toSearchResponse(MemberProfile profile) {
		Account account = profile.getAccount();
		return new ReceptionMemberSearchResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), profile.getProfileImageUrl(),
			account.getStatus(), activeMembership(account.getId()));
	}

	private ReceptionActiveMembershipResponse activeMembership(UUID memberAccountId) {
		var now = clock.instant();
		Date today = Date.valueOf(now.atZone(VIETNAM).toLocalDate());
		return db.query("""
			with current_membership as (
			    select * from memberships
			    where member_account_id=? and status='ACTIVE' and starts_at <= ? and ends_at > ?
			    order by ends_at desc, id limit 1
			)
			select membership.*, plan.display_name as plan_name, benefit.benefit_code,
			       exists (select 1 from membership_freeze_periods period
			               where period.membership_id=membership.id and period.status in ('SCHEDULED', 'ACTIVE')
			                 and coalesce(period.actual_start_date, period.scheduled_start_date) <= ?
			                 and coalesce(period.actual_end_date, period.scheduled_end_date) > ?) as frozen
			from current_membership membership
			left join membership_plans plan on plan.plan_code=membership.plan_code_snapshot
			left join membership_plan_benefits benefit on benefit.plan_code=plan.plan_code
			""", rows -> {
			if (!rows.next()) return null;
			UUID membershipId = rows.getObject("id", UUID.class);
			String offerName = rows.getString("offer_name_snapshot");
			String planName = rows.getString("plan_name");
			if (planName == null) planName = rows.getString("plan_code_snapshot");
			LocalDate start = rows.getTimestamp("starts_at").toInstant().atZone(VIETNAM).toLocalDate();
			LocalDate end = rows.getTimestamp("ends_at").toInstant().atZone(VIETNAM).toLocalDate();
			MembershipStatus status = MembershipStatus.valueOf(rows.getString("status"));
			boolean frozen = rows.getBoolean("frozen");
			Set<String> benefits = new TreeSet<>();
			do {
				String code = rows.getString("benefit_code");
				if (code != null) benefits.add(code);
			} while (rows.next());
			return new ReceptionActiveMembershipResponse(membershipId, offerName, planName, start, end,
				status, List.copyOf(benefits), frozen);
		}, memberAccountId, Timestamp.from(now), Timestamp.from(now), today, today);
	}

	private MemberProfileResponse toProfileResponse(MemberProfile profile) {
		Account account = profile.getAccount();
		return new MemberProfileResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus(),
			profile.getProfileImageUrl(), profile.getFitnessGoal(), profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone());
	}
}
