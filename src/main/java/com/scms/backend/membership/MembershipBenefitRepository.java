package com.scms.backend.membership;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipBenefitRepository extends JpaRepository<MembershipBenefit, String> {
	List<MembershipBenefit> findAllByStatusOrderByCode(MembershipBenefitStatus status);
	@Query("""
		select benefit from MembershipBenefit benefit
		left join fetch benefit.group benefitGroup
		order by coalesce(benefitGroup.displayOrder, benefit.displayOrder), benefit.displayOrder, benefit.code
		""")
	List<MembershipBenefit> findAllByOrderByCode();
	List<MembershipBenefit> findAllByCodeIn(Iterable<String> codes);
}
