package com.scms.backend.course;

@io.swagger.v3.oas.annotations.media.Schema(description = "Run completion is automatic after all effective units end and refund obligations are recorded; COMPLETED does not mean refunds have been paid.")
public enum CourseRunStatus {

	DRAFT,
	OPEN_FOR_REGISTRATION,
	CONFIRMED,
	IN_PROGRESS,
	SETTLEMENT_PENDING,
	COMPLETED,
	CANCELLED
}
