package com.scms.backend.account;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

	Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);

	Optional<Account> findByPhoneAndStatus(String phone, AccountStatus status);

	boolean existsByEmailIgnoreCaseAndStatusNot(String email, AccountStatus status);

	boolean existsByPhoneAndStatusNot(String phone, AccountStatus status);

	boolean existsByEmailIgnoreCaseAndStatusNotAndIdNot(String email, AccountStatus status, UUID id);

	boolean existsByPhoneAndStatusNotAndIdNot(String phone, AccountStatus status, UUID id);

	boolean existsByIdAndStatus(UUID id, AccountStatus status);

	boolean existsByIdAndRoleAndStatus(UUID id, AccountRole role, AccountStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.role = com.scms.backend.account.AccountRole.MANAGER and a.status = com.scms.backend.account.AccountStatus.ACTIVE")
	List<Account> findActiveManagersForUpdate();

}
