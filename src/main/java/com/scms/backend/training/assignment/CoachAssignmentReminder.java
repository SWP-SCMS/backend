package com.scms.backend.training.assignment;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciles assignment needs, including memberships activated by the lifecycle job.
 */
@Service
public class CoachAssignmentReminder {

	private final JdbcTemplate db;

	private final Clock clock;

	CoachAssignmentReminder(JdbcTemplate db, Clock clock) {
		this.db = db;
		this.clock = clock;
		this.coachAssignmentReminderRepository = new CoachAssignmentReminderRepository(this.db);
	}

	@Scheduled(fixedDelayString = "${scms.assignment-reminder-delay-ms:30000}", initialDelayString = "${scms.assignment-reminder-initial-delay-ms:5000}")
	@Transactional
	public void notifyManagers() {
		var now = clock.instant();
		var today = now.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
		// The membership and last ended assignment identify one assignment need.
		// A retry or another application instance cannot enqueue it twice.
		coachAssignmentReminderRepository.notifyManagersInsertNotificationOutbox(Timestamp.from(now), Timestamp.from(now), today, today);
	}

	private final CoachAssignmentReminderRepository coachAssignmentReminderRepository;
}
