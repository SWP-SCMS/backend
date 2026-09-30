package com.scms.backend.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);

	Optional<Account> findByPhoneAndStatus(String phone, AccountStatus status);

	boolean existsByEmailIgnoreCaseAndStatusNot(String email, AccountStatus status);

	boolean existsByPhoneAndStatusNot(String phone, AccountStatus status);

	boolean existsByEmailIgnoreCaseAndStatusNotAndIdNot(String email, AccountStatus status, UUID id);

	boolean existsByPhoneAndStatusNotAndIdNot(String phone, AccountStatus status, UUID id);

	boolean existsByIdAndStatus(UUID id, AccountStatus status);
}
