package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipOfferRepository extends JpaRepository<MembershipOffer, UUID> {

	@Query("""
		select offer from MembershipOffer offer
		join fetch offer.plan plan
		where offer.status = :status
		order by plan.planCode asc, offer.durationDays asc, offer.name asc, offer.id asc
		""")
	List<MembershipOffer> findAllByStatus(@Param("status") MembershipOfferStatus status);

	@Query("""
		select offer from MembershipOffer offer
		join fetch offer.plan plan
		where offer.status = :status and plan.planCode = :planCode
		order by plan.planCode asc, offer.durationDays asc, offer.name asc, offer.id asc
		""")
	List<MembershipOffer> findAllByStatusAndPlanCode(@Param("status") MembershipOfferStatus status,
		@Param("planCode") MembershipPlanCode planCode);
}
