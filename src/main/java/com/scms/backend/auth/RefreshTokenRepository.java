package com.scms.backend.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select token from RefreshToken token join fetch token.account where token.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Query("select token from RefreshToken token where token.account.id = :accountId")
	List<RefreshToken> findAllByAccountId(@Param("accountId") UUID accountId);
}
