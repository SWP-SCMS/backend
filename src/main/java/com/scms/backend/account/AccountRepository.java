package com.scms.backend.account;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

	Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);

	Optional<Account> findByPhoneAndStatus(String phone, AccountStatus status);

	boolean existsByEmailIgnoreCaseAndStatusNot(String email, AccountStatus status);

	boolean existsByPhoneAndStatusNot(String phone, AccountStatus status);

	boolean existsByEmailIgnoreCaseAndStatusNotAndIdNot(String email, AccountStatus status, UUID id);

	boolean existsByPhoneAndStatusNotAndIdNot(String phone, AccountStatus status, UUID id);

	boolean existsByIdAndStatus(UUID id, AccountStatus status);

	boolean existsByIdAndStatusAndAuthVersion(UUID id, AccountStatus status, long authVersion);

	boolean existsByIdAndRoleAndStatus(UUID id, AccountRole role, AccountStatus status);

	boolean existsByRole(AccountRole role);

	@Query("select count(a) > 0 from Account a where lower(a.email) = lower(:email) and "
		+ "(a.status <> com.scms.backend.account.AccountStatus.INACTIVE or a.emailVerificationRequired = true)")
	boolean existsByOccupiedEmail(@Param("email") String email);

	@Query("select count(a) > 0 from Account a where a.phone = :phone and "
		+ "(a.status <> com.scms.backend.account.AccountStatus.INACTIVE or a.emailVerificationRequired = true)")
	boolean existsByOccupiedPhone(@Param("phone") String phone);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where lower(a.email) = lower(:email)")
	List<Account> findByEmailForUpdate(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where lower(a.email)=lower(:email) and a.phone=:phone and "
		+ "a.role=com.scms.backend.account.AccountRole.MEMBER and a.status=com.scms.backend.account.AccountStatus.INACTIVE "
		+ "and a.emailVerificationRequired=false and a.registrationExpiresAt is not null order by a.registrationExpiresAt desc")
	List<Account> findExpiredSelfRegistrationForUpdate(@Param("email") String email,@Param("phone") String phone);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.role = com.scms.backend.account.AccountRole.MANAGER and a.status = com.scms.backend.account.AccountStatus.ACTIVE order by a.id")
	List<Account> findActiveManagersForUpdate();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdForUpdate(UUID id);

}
