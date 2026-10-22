package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.training.CoachAssignmentRepository;
import com.scms.backend.training.CoachAssignmentStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WaitlistService {
	private final SessionWaitlistRepository waitlists;
	private final ClassSessionRepository sessions;
	private final BookingRepository bookings;
	private final DisciplineRepository disciplines;
	private final AccountRepository accounts;
	private final CoachAssignmentRepository coachAssignments;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	WaitlistService(SessionWaitlistRepository waitlists, ClassSessionRepository sessions, BookingRepository bookings,
			DisciplineRepository disciplines, AccountRepository accounts, CoachAssignmentRepository coachAssignments,
			AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.waitlists=waitlists; this.sessions=sessions; this.bookings=bookings; this.disciplines=disciplines;
		this.accounts=accounts; this.coachAssignments=coachAssignments; this.audits=audits;
		this.notifications=notifications; this.clock=clock;
	}

	@Transactional WaitlistResponse join(UUID memberId, UUID sessionId) { return join(memberId, memberId, sessionId, "MEMBER"); }
	@Transactional WaitlistResponse joinForMember(UUID receptionistId, UUID memberId, UUID sessionId) {
		ensureRole(receptionistId, AccountRole.RECEPTIONIST); return join(receptionistId, memberId, sessionId, "RECEPTIONIST");
	}
	private WaitlistResponse join(UUID actorId, UUID memberId, UUID sessionId, String source) {
		Instant now=clock.instant(); expireStarted(now);
		ClassSession session=sessions.findByIdForUpdate(sessionId).orElseThrow(BookingException::sessionNotFound);
		ensureActiveMember(memberId);
		if(session.getStatus()!=ClassSessionStatus.SCHEDULED || !session.getStartTime().isAfter(now)) throw BookingException.sessionUnavailable();
		disciplines.findBySessionIdForUpdate(sessionId).orElseThrow(BookingException::sessionNotFound);
		if(bookings.countByClassSessionIdAndStatus(sessionId, BookingStatus.BOOKED)<session.getCapacity()) throw WaitlistException.sessionNotFull();
		validateEligibility(memberId, session);
		if(waitlists.findBySessionIdAndMemberIdAndStatus(sessionId,memberId,WaitlistStatus.WAITING).isPresent()) throw WaitlistException.duplicate();
		SessionWaitlist entry=new SessionWaitlist(UUID.randomUUID(),sessionId,memberId,actorId,source,now);
		try { waitlists.saveAndFlush(entry); } catch(DataIntegrityViolationException exception){ throw WaitlistException.duplicate(); }
		audits.save(new AuditEvent(UUID.randomUUID(),actorId,"WAITLIST_JOINED","SESSION_WAITLIST",entry.getId(),
			Map.of("sessionId",sessionId.toString(),"memberId",memberId.toString(),"status","WAITING")));
		return response(entry);
	}

	@Transactional WaitlistResponse leave(UUID memberId, UUID sessionId) { return leave(memberId,memberId,sessionId); }
	@Transactional WaitlistResponse leaveForMember(UUID receptionistId,UUID memberId,UUID sessionId){
		ensureRole(receptionistId,AccountRole.RECEPTIONIST); return leave(receptionistId,memberId,sessionId);
	}
	private WaitlistResponse leave(UUID actorId,UUID memberId,UUID sessionId){
		if(actorId.equals(memberId)) ensureRole(memberId,AccountRole.MEMBER); else ensureActiveMember(memberId);
		SessionWaitlist entry=waitlists.findWaitingForUpdate(sessionId,memberId).orElseThrow(WaitlistException::notFound);
		entry.cancel("LEFT_WAITLIST",clock.instant());
		audits.save(new AuditEvent(UUID.randomUUID(),actorId,"WAITLIST_LEFT","SESSION_WAITLIST",entry.getId(),
			Map.of("sessionId",sessionId.toString(),"memberId",memberId.toString(),"status","CANCELLED")));
		return response(entry);
	}

	@Transactional List<WaitlistResponse> mine(UUID memberId){
		ensureRole(memberId,AccountRole.MEMBER); expireStarted(clock.instant());
		return waitlists.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(this::response).toList();
	}
	@Transactional List<WaitlistResponse> sessionList(UUID actorId,AccountRole role,UUID sessionId){
		ensureRole(actorId,role); expireStarted(clock.instant());
		return waitlists.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(this::response).toList();
	}

	void expireStarted(Instant now){
		for(SessionWaitlist entry:waitlists.findExpiredForUpdate(now)){
			entry.expire(now); waitlists.save(entry);
			audits.save(new AuditEvent(UUID.randomUUID(),null,"WAITLIST_EXPIRED","SESSION_WAITLIST",entry.getId(),
				Map.of("sessionId",entry.getSessionId().toString(),"reasonCode","SESSION_STARTED")));
		}
	}

	UUID validateEligibility(UUID memberId,ClassSession session){
		ensureActiveMember(memberId);
		if(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId,session.getId(),BookingStatus.BOOKED)) throw BookingException.duplicate();
		if(bookings.existsBookedOverlap(memberId,session.getStartTime(),session.getEndTime())) throw BookingException.timeConflict();
		if(session.getSportClass().getClassType()==SportClassType.PT_1_1 && coachAssignments
			.findByMemberAccountIdAndCoachAccountIdAndStatus(memberId,session.getTeachingCoach().getId(),CoachAssignmentStatus.ACTIVE).isEmpty())
			throw BookingException.ptCoachAssignmentMismatch();
		List<BookingRepository.BookingBenefitEligibilityView> eligibility=bookings.findBookingBenefitEligibility(memberId,session.getId(),session.getStartTime());
		List<String> required=eligibility.stream().map(BookingRepository.BookingBenefitEligibilityView::getRequiredBenefitCode).distinct().sorted().toList();
		Map<UUID,List<BookingRepository.BookingBenefitEligibilityView>> byMembership=new LinkedHashMap<>();
		eligibility.stream().filter(row->row.getMembershipId()!=null).forEach(row->byMembership.computeIfAbsent(row.getMembershipId(),ignored->new java.util.ArrayList<>()).add(row));
		UUID membershipId=byMembership.entrySet().stream().filter(entry->entry.getValue().stream().allMatch(BookingRepository.BookingBenefitEligibilityView::getProvided)).map(Map.Entry::getKey).findFirst().orElse(null);
		if(membershipId==null){
			List<String> missing=byMembership.values().stream().map(rows->rows.stream().filter(row->!row.getProvided()).map(BookingRepository.BookingBenefitEligibilityView::getRequiredBenefitCode).sorted().toList()).min(Comparator.comparingInt(List::size)).orElse(required);
			throw BookingException.benefitRequired(required,missing);
		}
		return membershipId;
	}
	private Account ensureActiveMember(UUID id){ Account account=accounts.findById(id).orElseThrow(BookingException::memberNotActive); if(account.getRole()!=AccountRole.MEMBER||account.getStatus()!=AccountStatus.ACTIVE) throw BookingException.memberNotActive(); return account; }
	private void ensureRole(UUID id,AccountRole role){ if(!accounts.existsByIdAndRoleAndStatus(id,role,AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException(); }
	private WaitlistResponse response(SessionWaitlist entry){ long ahead=entry.getStatus()==WaitlistStatus.WAITING?waitlists.countAhead(entry.getSessionId(),entry.getCreatedAt(),entry.getId()):0; return WaitlistResponse.from(entry,ahead); }
}
