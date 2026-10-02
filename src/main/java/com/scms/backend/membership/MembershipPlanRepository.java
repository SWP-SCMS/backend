package com.scms.backend.membership;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, MembershipPlanCode> { }
