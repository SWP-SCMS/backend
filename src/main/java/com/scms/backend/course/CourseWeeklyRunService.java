package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import com.scms.backend.account.*;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.scheduling.SharedResourceLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseWeeklyRunService {
    private final CourseManagementService management;
    private final AccountRepository accounts;
    private final SharedResourceLockService resourceLocks;
    private final Clock clock;

    CourseWeeklyRunService(CourseManagementService management, AccountRepository accounts,
            SharedResourceLockService resourceLocks, Clock clock) {
        this.management = management; this.accounts = accounts; this.resourceLocks = resourceLocks; this.clock = clock;
    }

    @Transactional(readOnly = true)
    CourseWeeklyPreview preview(UUID managerId, CourseWeeklyScheduleRequest request) {
        ensureManager(managerId);
        return CourseWeeklySchedule.generate(request);
    }

    @Transactional
    CourseWeeklyRunResponse create(UUID managerId, CourseWeeklyRunCreateRequest request) {
        ensureManager(managerId);
        if (request == null || request.primaryCoachId() == null || request.roomId() == null)
            throw CourseException.validation("schedule", "Vui lòng chọn huấn luyện viên và phòng học.");
        var calendar = CourseWeeklySchedule.generate(request.schedule());
        if (!calendar.plannedStartAt().isAfter(clock.instant()))
            throw CourseException.validation("openingDate", "Ngày giờ khai giảng phải ở tương lai.");
        if (request.tuitionVnd() == null || request.tuitionVnd().compareTo(BigInteger.valueOf(request.schedule().totalUnits())) < 0)
            throw CourseException.validation("tuitionVnd", "Học phí phải đủ phân bổ ít nhất 1 VND cho mỗi buổi.");
        // Same resource-before-run ordering as manual session creation/rescheduling.
        resourceLocks.lock(List.of(request.primaryCoachId()), List.of(request.roomId()));
        var run = management.createRun(managerId, new CourseRunCreateRequest(request.runCode(), request.courseId(),
            request.primaryCoachId(), request.tuitionVnd(), request.capacity(), request.minimumEnrollment(),
            request.registrationOpensAt(), request.registrationClosesAt(), calendar.plannedStartAt(), calendar.plannedEndAt(),
            request.schedule().totalUnits(), request.namedCoachCommitment()));
        var split = request.tuitionVnd().divideAndRemainder(BigInteger.valueOf(calendar.sessions().size()));
        for (var slot : calendar.sessions()) {
            var allocation = slot.unitNumber() == calendar.sessions().size() ? split[0].add(split[1]) : split[0];
            management.createSession(managerId, run.id(), new CourseSessionCreateRequest(slot.unitNumber(),
                request.roomId(), request.primaryCoachId(), slot.startAt(), slot.endAt(), allocation));
        }
        return new CourseWeeklyRunResponse(management.getRun(managerId, run.id()), management.listSessions(managerId, run.id()));
    }

    private void ensureManager(UUID id) {
        if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE))
            throw new InvalidAuthenticatedAccountException();
    }
}
