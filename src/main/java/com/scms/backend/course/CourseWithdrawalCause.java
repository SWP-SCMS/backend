package com.scms.backend.course;

public enum CourseWithdrawalCause {

	VOLUNTARY, START_DELAY_OVER_7_DAYS, MEMBER_SCHEDULE_CONFLICT, REDUCED_UNIT_COUNT, CENTER_SESSION_RESCHEDULE;

	public boolean fullUnusedRefund() {
		return this == CENTER_SESSION_RESCHEDULE || this == MEMBER_SCHEDULE_CONFLICT;
	}
}
