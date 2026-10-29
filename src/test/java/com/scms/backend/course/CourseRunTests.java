package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

class CourseRunTests {

	private static final Instant REGISTRATION_OPENS = Instant.parse("2026-11-01T00:00:00Z");
	private static final Instant REGISTRATION_CLOSES = Instant.parse("2026-11-15T00:00:00Z");
	private static final Instant PLANNED_START = Instant.parse("2026-11-20T00:00:00Z");
	private static final Instant PLANNED_END = Instant.parse("2027-01-20T00:00:00Z");

	@Test
	void followsTheRequiredLifecycleAndSnapshotsPolicyWhenRegistrationOpens() {
		CourseRun run = validRun();

		run.openForRegistration("POLICY-2026-01");
		assertThat(run.getStatus()).isEqualTo(CourseRunStatus.OPEN_FOR_REGISTRATION);
		assertThat(run.getPolicyVersionSnapshot()).isEqualTo("POLICY-2026-01");

		run.confirm();
		run.start();
		run.markSettlementPending();
		run.complete();

		assertThat(run.getStatus()).isEqualTo(CourseRunStatus.COMPLETED);
	}

	@ParameterizedTest
	@EnumSource(value = CourseRunStatus.class, names = {
		"DRAFT", "OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS"
	})
	void cancellationIsAllowedOnlyFromCancellableStates(CourseRunStatus state) {
		CourseRun run = validRun();
		moveTo(run, state);

		run.cancel();

		assertThat(run.getStatus()).isEqualTo(CourseRunStatus.CANCELLED);
	}

	@Test
	void rejectsSkippedAndTerminalTransitionsWithStableCourseError() {
		CourseRun draft = validRun();
		assertInvalidTransition(draft::confirm);

		CourseRun settlementPending = validRun();
		moveTo(settlementPending, CourseRunStatus.SETTLEMENT_PENDING);
		assertInvalidTransition(settlementPending::cancel);

		CourseRun completed = validRun();
		moveTo(completed, CourseRunStatus.COMPLETED);
		assertInvalidTransition(completed::cancel);

		CourseRun cancelled = validRun();
		cancelled.cancel();
		assertInvalidTransition(() -> cancelled.openForRegistration("POLICY-2026-02"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidOpeningCases")
	void openingRejectsInvalidImmutablePricingAndPolicyFields(String description, CourseRun run, String policy) {
		assertThatThrownBy(() -> run.openForRegistration(policy))
			.isInstanceOfSatisfying(CourseException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(exception.getCode()).isEqualTo("COURSE_RUN_INVALID_OPENING");
			});
		assertThat(run.getStatus()).isEqualTo(CourseRunStatus.DRAFT);
		assertThat(run.getPolicyVersionSnapshot()).isNull();
	}

	@Test
	void minimumEnrollmentCanChangeOnlyWhileDraft() {
		CourseRun run = validRun();

		run.changeMinimumEnrollment(4);
		assertThat(run.getMinimumEnrollment()).isEqualTo(4);
		run.openForRegistration("POLICY-2026-01");

		assertThatThrownBy(() -> run.changeMinimumEnrollment(5))
			.isInstanceOfSatisfying(CourseException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getCode()).isEqualTo("COURSE_RUN_IMMUTABLE");
			});
		assertThat(run.getMinimumEnrollment()).isEqualTo(4);
	}

	private static Stream<Arguments> invalidOpeningCases() {
		return Stream.of(
			Arguments.of("tuition must be positive", run(BigInteger.ZERO, 20, 3,
				REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END, 12), "POLICY-2026-01"),
			Arguments.of("capacity must be positive", run(BigInteger.valueOf(5_000_000), 0, 3,
				REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END, 12), "POLICY-2026-01"),
			Arguments.of("minimum enrollment must be positive", run(BigInteger.valueOf(5_000_000), 20, 0,
				REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END, 12), "POLICY-2026-01"),
			Arguments.of("minimum enrollment cannot exceed capacity", run(BigInteger.valueOf(5_000_000), 20, 21,
				REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END, 12), "POLICY-2026-01"),
			Arguments.of("registration must close after opening", run(BigInteger.valueOf(5_000_000), 20, 3,
				REGISTRATION_OPENS, REGISTRATION_OPENS, PLANNED_START, PLANNED_END, 12), "POLICY-2026-01"),
			Arguments.of("registration must close by planned start", run(BigInteger.valueOf(5_000_000), 20, 3,
				REGISTRATION_OPENS, PLANNED_START.plusSeconds(1), PLANNED_START, PLANNED_END, 12), "POLICY-2026-01"),
			Arguments.of("planned end must follow planned start", run(BigInteger.valueOf(5_000_000), 20, 3,
				REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_START, 12), "POLICY-2026-01"),
			Arguments.of("total units must be positive", run(BigInteger.valueOf(5_000_000), 20, 3,
				REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END, 0), "POLICY-2026-01"),
			Arguments.of("policy snapshot is required", validRun(), " ")
		);
	}

	private static CourseRun validRun() {
		return run(BigInteger.valueOf(5_000_000), 20, 3,
			REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END, 12);
	}

	private static CourseRun run(BigInteger tuitionVnd, int capacity, int minimumEnrollment,
			Instant registrationOpensAt, Instant registrationClosesAt, Instant plannedStartAt,
			Instant plannedEndAt, int totalUnits) {
		return new CourseRun(UUID.randomUUID(), "RUN-2026-01", null, null, tuitionVnd, capacity,
			minimumEnrollment, registrationOpensAt, registrationClosesAt, plannedStartAt, plannedEndAt,
			totalUnits, true);
	}

	private static void moveTo(CourseRun run, CourseRunStatus state) {
		if (state == CourseRunStatus.DRAFT) return;
		run.openForRegistration("POLICY-2026-01");
		if (state == CourseRunStatus.OPEN_FOR_REGISTRATION) return;
		run.confirm();
		if (state == CourseRunStatus.CONFIRMED) return;
		run.start();
		if (state == CourseRunStatus.IN_PROGRESS) return;
		run.markSettlementPending();
		if (state == CourseRunStatus.SETTLEMENT_PENDING) return;
		run.complete();
	}

	private static void assertInvalidTransition(Runnable transition) {
		assertThatThrownBy(transition::run)
			.isInstanceOfSatisfying(CourseException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getCode()).isEqualTo("COURSE_RUN_INVALID_TRANSITION");
			});
	}
}
