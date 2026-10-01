package com.scms.backend.manager;

import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.auth.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetPasswordService {

	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final RefreshTokenService refreshTokens;
	private final PasswordEncoder encoder;
	private final MemberProfileRepository profiles;

	ResetPasswordService(AccountRepository accounts, AuditEventRepository audits, RefreshTokenService refreshTokens,
			PasswordEncoder encoder, MemberProfileRepository profiles) {
		this.accounts = accounts;
		this.audits = audits;
		this.refreshTokens = refreshTokens;
		this.encoder = encoder;
		this.profiles = profiles;
	}

	@Transactional
	public void managerReset(UUID actorId, UUID targetId) {
		ensureActive(actorId, AccountRole.MANAGER);
		Account target = target(targetId);
		if (actorId.equals(targetId)) {
			throw new StaffStatusConflictException("Use the personal password change flow to change your own password");
		}
		reset(actorId, target);
	}

	@Transactional
	public void receptionistResetMember(UUID actorId, String memberCode) {
		ensureActive(actorId, AccountRole.RECEPTIONIST);
		Account target = profiles.findVisibleMemberByCode(memberCode, AccountRole.MEMBER, java.util.List.of(AccountStatus.ACTIVE))
			.map(profile -> profile.getAccount())
			.orElseThrow(MemberAccountNotFoundException::new);
		reset(actorId, target);
	}

	private void reset(UUID actorId, Account target) {
		target.changePassword(encoder.encode(target.getPhone()));
		accounts.flush();
		refreshTokens.revokeAll(target.getId());
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "PASSWORD_RESET", "ACCOUNT", target.getId(),
			null, Map.of("role", (Object) target.getRole().name()), Map.of("passwordReset", (Object) true)));
	}

	private Account target(UUID id) {
		return accounts.findById(id).filter(account -> account.getStatus() == AccountStatus.ACTIVE)
			.orElseThrow(StaffAccountNotFoundException::new);
	}

	private void ensureActive(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}
}
