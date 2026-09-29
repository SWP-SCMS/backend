package com.scms.backend.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);

	Optional<Account> findByPhoneAndStatus(String phone, AccountStatus status);

	boolean existsByIdAndStatus(UUID id, AccountStatus status);
}
