package com.scms.backend.scheduling;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLifecycleService {

	private final ClassSessionRepository sessions;
	private final AttendanceRepository attendance;
	private final WaitlistService waitlists;

	SessionLifecycleService(ClassSessionRepository sessions, AttendanceRepository attendance,
			WaitlistService waitlists) {
		this.sessions = sessions;
		this.attendance = attendance;
		this.waitlists = waitlists;
	}

	@Transactional
	public void catchUp(Instant now) {
		sessions.advanceScheduledToInProgress(now);
		waitlists.expireStarted(now);
		attendance.insertAbsentForStartedBookings(now);
		sessions.advanceInProgressToCompleted(now);
	}
}
