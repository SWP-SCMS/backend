package com.scms.backend.membership;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, String> {
	@Query("select distinct plan from MembershipPlan plan left join fetch plan.benefits order by plan.planCode")
	List<MembershipPlan> findAllWithBenefits();

	@Query("select distinct plan from MembershipPlan plan left join fetch plan.benefits where plan.planCode = :code")
	Optional<MembershipPlan> findByIdWithBenefits(@Param("code") String code);
}
