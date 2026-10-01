package com.scms.backend.account;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

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

	@Query("""
		select a from Account a
		where a.role in (com.scms.backend.account.AccountRole.COACH,
			com.scms.backend.account.AccountRole.RECEPTIONIST,
			com.scms.backend.account.AccountRole.MANAGER)
		and (:role is null or a.role = :role)
		and (:status is null or a.status = :status)
		and (:query is null or lower(a.fullName) like lower(concat('%', :query, '%'))
			or lower(a.email) like lower(concat('%', :query, '%'))
			or a.phone like concat('%', :query, '%'))
		order by a.fullName asc, a.id asc
		""")
	Page<Account> searchStaff(@Param("query") String query, @Param("role") AccountRole role,
			@Param("status") AccountStatus status, Pageable pageable);
}
