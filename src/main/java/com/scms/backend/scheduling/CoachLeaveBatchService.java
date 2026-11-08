package com.scms.backend.scheduling;

import java.util.*;
import com.scms.backend.course.CourseLeaveBatchActions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoachLeaveBatchService {
    private final CoachLeaveImpactService impacts;
    private final CoachLeaveRepository leaves;
    private final ClassSessionRepository sessions;
    private final ClassSessionService assignments;
    private final ClassSessionCancellationService cancellations;
    private final CourseLeaveBatchActions courses;
    private final SharedResourceLockService resources;
    CoachLeaveBatchService(CoachLeaveImpactService impacts, CoachLeaveRepository leaves, ClassSessionRepository sessions,
            ClassSessionService assignments, ClassSessionCancellationService cancellations, CourseLeaveBatchActions courses,
            SharedResourceLockService resources) {
        this.impacts=impacts; this.leaves=leaves; this.sessions=sessions; this.assignments=assignments;
        this.cancellations=cancellations; this.courses=courses; this.resources=resources;
    }
    record Selection(String type, UUID id, Long expectedVersion) {}
    record Command(List<Selection> sessions, String action, UUID coachId, String reason) {}
    record Result(int processedCount) {}

    private List<CoachLeaveImpactService.AffectedSession> selected(CoachLeaveImpactService.Impact impact, List<Selection> selection) {
        if (impact.request().status()!=CoachLeaveStatus.PENDING)
            throw CoachLeaveException.conflict("COACH_LEAVE_NOT_PENDING", "Only pending leave requests can be resolved.");
        if (selection==null || selection.isEmpty() || selection.size()>100)
            throw CoachLeaveException.validation("sessions", "Select between 1 and 100 sessions.");
        Set<String> keys=new HashSet<>();
        List<CoachLeaveImpactService.AffectedSession> result=new ArrayList<>();
        for (Selection item:selection) {
            if (item==null || item.id()==null || item.type()==null || item.expectedVersion()==null || item.expectedVersion()<0
                    || !keys.add(item.type()+item.id())) throw CoachLeaveException.validation("sessions", "Invalid or duplicate session.");
            var session=impact.sessions().stream().filter(s->s.id().equals(item.id()) && s.type().equals(item.type())).findFirst()
                .orElseThrow(()->CoachLeaveException.conflict("COACH_LEAVE_BATCH_CHANGED", "Selected sessions are no longer affected by this leave."));
            if (session.version()!=item.expectedVersion()) throw CoachLeaveException.conflict("COACH_LEAVE_BATCH_CHANGED", "Selected session version changed.");
            result.add(session);
        }
        if (result.stream().map(s->s.type()).distinct().count()!=1)
            throw CoachLeaveException.validation("sessions", "Select sessions from one tab at a time.");
        result.sort(Comparator.comparing((CoachLeaveImpactService.AffectedSession s)->Objects.toString(s.courseRunId(), ""))
            .thenComparing(s->s.id().toString()));
        return result;
    }
    private void requireNonOverlapping(List<CoachLeaveImpactService.AffectedSession> selected) {
        for (int i=0;i<selected.size();i++) for (int j=i+1;j<selected.size();j++) {
            var a=selected.get(i); var b=selected.get(j);
            if(a.startTime().isBefore(b.endTime()) && b.startTime().isBefore(a.endTime()))
                throw CoachLeaveException.conflict("COACH_LEAVE_BATCH_OVERLAP", "Selected sessions overlap; select a smaller group.");
        }
    }
    @Transactional
    List<AvailableCoachResponse> available(UUID manager, UUID request, Command command) {
        if(command==null) throw CoachLeaveException.validation("request", "Request is required.");
        var impact=impacts.detail(manager,request);
        var selected=selected(impact,command.sessions());
        requireNonOverlapping(selected);
        List<AvailableCoachResponse> common=null;
        for(var session:selected) {
            if(!session.canReassign()) return List.of();
            var candidates=impacts.available(impact.request().coachId(),session);
            if(common==null) common=new ArrayList<>(candidates);
            else common.removeIf(coach->candidates.stream().noneMatch(candidate->candidate.coachAccountId().equals(coach.coachAccountId())));
            if(common.isEmpty()) break;
        }
        return common==null ? List.of() : common;
    }
    @Transactional
    Result resolve(UUID manager, UUID request, Command command) {
        if(command==null || !List.of("reassign","cancel").contains(Objects.toString(command.action(), "")))
            throw CoachLeaveException.validation("action", "Choose reassign or cancel.");
        var impact=impacts.detail(manager,request);
        var lockedLeave=leaves.findByIdForUpdate(request).orElseThrow(CoachLeaveException::notFound);
        if(lockedLeave.getStatus()!=CoachLeaveStatus.PENDING)
            throw CoachLeaveException.conflict("COACH_LEAVE_NOT_PENDING", "Only pending leave requests can be resolved.");
        var selected=selected(impact,command.sessions());
        boolean reassign=command.action().equals("reassign");
        if(reassign) {
            if(command.coachId()==null || command.coachId().equals(impact.request().coachId()))
                throw CoachLeaveException.validation("coachId", "Choose a replacement Coach.");
            requireNonOverlapping(selected);
            var coachIds=new ArrayList<UUID>(); coachIds.add(command.coachId()); selected.forEach(s->coachIds.add(s.coachId()));
            resources.lock(coachIds,selected.stream().map(s->s.roomId()).filter(Objects::nonNull).toList());
        } else if(command.reason()==null || command.reason().isBlank() || command.reason().length()>1000)
            throw CoachLeaveException.validation("reason", "A cancellation reason is required (maximum 1000 characters).");
        for(var session:selected) {
            if(reassign ? !session.canReassign() : !session.canCancel() || session.memberCount()>0)
                throw CoachLeaveException.conflict("COACH_LEAVE_BATCH_CHANGED", "A selected session is no longer eligible.");
        }
        for(var session:selected) {
            try {
                if(session.type().equals("COURSE")) {
                    if(reassign) courses.reassign(manager,session.courseRunId(),session.id(),session.version(),session.roomId(),command.coachId(),session.startTime(),session.endTime());
                    else courses.cancel(manager,session.courseRunId(),session.id(),session.version());
                } else if(reassign) assignments.updateAssignment(manager,session.id(),new ClassSessionAssignmentRequest(command.coachId(),null,session.version()));
                else {
                    var locked=sessions.findByIdForUpdate(session.id()).orElseThrow(CoachLeaveException::notFound);
                    if(!Objects.equals(locked.getVersion(),session.version())) throw RecurringScheduleException.versionConflict();
                    cancellations.cancel(manager,session.id(),new ClassSessionCancellationRequest(command.reason().trim()));
                }
            } catch (RuntimeException exception) {
                throw CoachLeaveException.batchFailure(session.id(), session.name(), exception);
            }
        }
        return new Result(selected.size());
    }
}
