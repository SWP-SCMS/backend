package com.scms.backend.scheduling;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLifecycleService {

	private final ClassSessionRepository sessions;
	private final AttendanceRepository attendance;

	SessionLifecycleService(ClassSessionRepository sessions, AttendanceRepository attendance) {
		this.sessions = sessions;
		this.attendance = attendance;
	}

	@Transactional
	public void catchUp(Instant now) {
		sessions.advanceScheduledToInProgress(now);
		attendance.insertAbsentForStartedBookings(now);
		sessions.advanceInProgressToCompleted(now);
	}
}
