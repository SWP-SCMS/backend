package com.scms.backend.membership;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipBenefitRepository extends JpaRepository<MembershipBenefit, String> {
	List<MembershipBenefit> findAllByStatusOrderByCode(MembershipBenefitStatus status);
	List<MembershipBenefit> findAllByOrderByCode();
	List<MembershipBenefit> findAllByCodeIn(Iterable<String> codes);
}
