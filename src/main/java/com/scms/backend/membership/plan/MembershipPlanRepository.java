package com.scms.backend.membership.plan;

import com.scms.backend.membership.MembershipPlan;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, String> {

	@Query("select distinct plan from MembershipPlan plan left join fetch plan.benefits benefit left join fetch benefit.group order by plan.planCode")
	List<MembershipPlan> findAllWithBenefits();

	@Query("select distinct plan from MembershipPlan plan left join fetch plan.benefits benefit left join fetch benefit.group where plan.planCode = :code")
	Optional<MembershipPlan> findByIdWithBenefits(@Param("code") String code);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select plan from MembershipPlan plan where plan.planCode = :code")
	Optional<MembershipPlan> findByPlanCodeForUpdate(@Param("code") String code);

	@Query(value = "select status from membership_plans where plan_code = :code for update", nativeQuery = true)
	Optional<String> findStatusByPlanCodeForUpdate(@Param("code") String code);
}
