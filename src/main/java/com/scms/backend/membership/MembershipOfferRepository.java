package com.scms.backend.membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipOfferRepository extends JpaRepository<MembershipOffer, UUID> {

	@Query("""
		select distinct offer from MembershipOffer offer
		join fetch offer.plan plan
		left join fetch plan.benefits
		order by plan.planCode asc, offer.durationDays asc, offer.name asc, offer.id asc
		""")
	List<MembershipOffer> findAllWithPlanAndBenefits();

	@Query("""
		select distinct offer from MembershipOffer offer
		join fetch offer.plan plan
		left join fetch plan.benefits
		where offer.id = :offerId
		""")
	Optional<MembershipOffer> findByIdWithPlanAndBenefits(@Param("offerId") UUID offerId);

	@Query("""
		select distinct offer from MembershipOffer offer
		join fetch offer.plan plan
		left join fetch plan.benefits
		where offer.status = :status and plan.status = 'ACTIVE'
		order by plan.planCode asc, offer.durationDays asc, offer.name asc, offer.id asc
		""")
	List<MembershipOffer> findAllByStatus(@Param("status") MembershipOfferStatus status);

	@Query("""
		select distinct offer from MembershipOffer offer
		join fetch offer.plan plan
		left join fetch plan.benefits
		where offer.status = :status and plan.status = 'ACTIVE' and plan.planCode = :planCode
		order by plan.planCode asc, offer.durationDays asc, offer.name asc, offer.id asc
		""")
	List<MembershipOffer> findAllByStatusAndPlanCode(@Param("status") MembershipOfferStatus status,
		@Param("planCode") String planCode);

	@Query("""
		select offer from MembershipOffer offer
		join fetch offer.plan
		where offer.id = :offerId and offer.status = :status
		""")
	Optional<MembershipOffer> findByIdAndStatus(@Param("offerId") UUID offerId,
		@Param("status") MembershipOfferStatus status);

	@Query("""
		select distinct offer from MembershipOffer offer
		join fetch offer.plan plan
		left join fetch plan.benefits
		where offer.id = :offerId and offer.status = :status and plan.status = 'ACTIVE'
		""")
	Optional<MembershipOffer> findByIdAndStatusAndActivePlan(@Param("offerId") UUID offerId,
		@Param("status") MembershipOfferStatus status);

	@Query(value = """
		select offer.plan_code
		from membership_offers offer
		join membership_plans plan on plan.plan_code = offer.plan_code
		where offer.id = :offerId
		  and offer.status = 'ACTIVE'
		  and plan.status = 'ACTIVE'
		for update of offer, plan
		""", nativeQuery = true)
	Optional<String> findSellablePlanCodeForUpdate(@Param("offerId") UUID offerId);
}
