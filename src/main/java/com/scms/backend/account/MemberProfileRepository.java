package com.scms.backend.account;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, UUID>, JpaSpecificationExecutor<MemberProfile> {

	@Query("""
		select profile from MemberProfile profile
		join fetch profile.account account
		where profile.memberCode = :memberCode
		and account.role = :role
		and account.status in :statuses
		""")
	Optional<MemberProfile> findVisibleMemberByCode(@Param("memberCode") String memberCode,
			@Param("role") AccountRole role, @Param("statuses") Collection<AccountStatus> statuses);

	@Query("""
		select profile from MemberProfile profile
		join fetch profile.account account
		where account.phone = :phone
		and account.role = :role
		and account.status in :statuses
		""")
	Optional<MemberProfile> findVisibleMemberByPhone(@Param("phone") String phone,
			@Param("role") AccountRole role, @Param("statuses") Collection<AccountStatus> statuses);
}
