package com.scms.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.jupiter.api.Test;
import com.scms.backend.common.validation.PersonNameRules;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({ OpenApiConfiguration.class, SpringDocConfiguration.class,
	SpringDocSecurityConfiguration.class, SpringDocWebMvcConfiguration.class,
	GeneratedOpenApiTests.ControllerDependencyMockConfiguration.class })
class GeneratedOpenApiTests {
	private static final Set<String> UNAUTHORIZED_PROBLEM_CODES = Set.of(
		"UNAUTHENTICATED", "INVALID_TOKEN", "INVALID_CREDENTIALS", "INVALID_REFRESH_TOKEN",
		"INVALID_PENDING_SESSION", "INVALID_PASSWORD_RESET_SESSION", "PAYMENT_UNAUTHORIZED");
	private static final Set<String> COURSE_PROBLEM_CODES = Set.of(
		"VALIDATION_ERROR", "MALFORMED_REQUEST", "UNAUTHENTICATED", "INVALID_TOKEN", "FORBIDDEN",
		"COURSE_VALIDATION_ERROR", "COURSE_NOT_FOUND", "COURSE_CODE_CONFLICT", "COURSE_INVALID_TRANSITION",
		"COURSE_CHANGED", "COURSE_RUN_NOT_FOUND", "COURSE_RUN_CODE_CONFLICT", "COURSE_RUN_INVALID_OPENING",
		"COURSE_RUN_INVALID_TRANSITION", "COURSE_RUN_IMMUTABLE", "COURSE_RUN_CHANGED",
		"COURSE_SESSION_NOT_FOUND", "COURSE_SESSION_INVALID", "COURSE_SESSION_RESOURCE_CONFLICT",
		"COURSE_SESSION_ROOM_CAPACITY_CONFLICT", "COURSE_SESSION_CHANGED");
	private static final Set<String> READ_COLLECTION_RESPONSES = Set.of("200", "400", "401", "403");
	private static final Set<String> CREATE_RESPONSES = Set.of("201", "400", "401", "403", "409");
	private static final Set<String> READ_ITEM_RESPONSES = Set.of("200", "400", "401", "403", "404");
	private static final Set<String> MUTATE_ITEM_RESPONSES = Set.of("200", "400", "401", "403", "404", "409");
	private static final List<CourseOperationContract> COURSE_OPERATION_CONTRACTS = List.of(
		new CourseOperationContract("/manager/courses", "get", READ_COLLECTION_RESPONSES),
		new CourseOperationContract("/manager/courses", "post", CREATE_RESPONSES),
		new CourseOperationContract("/manager/courses/{courseId}", "get", READ_ITEM_RESPONSES),
		new CourseOperationContract("/manager/courses/{courseId}", "patch", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/courses/{courseId}/publish", "post", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/courses/{courseId}/deactivate", "post", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/courses/{courseId}/archive", "post", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs", "get", READ_COLLECTION_RESPONSES),
		new CourseOperationContract("/manager/course-runs", "post", Set.of("201", "400", "401", "403", "404", "409")),
		new CourseOperationContract("/manager/course-runs/{runId}", "get", READ_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs/{runId}", "patch", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs/{runId}/open-registration", "post", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs/{runId}/cancel", "post", MUTATE_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs/{runId}/sessions", "get", READ_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs/{runId}/sessions", "post",
			Set.of("201", "400", "401", "403", "404", "409")),
		new CourseOperationContract("/manager/course-runs/{runId}/sessions/{sessionId}", "get", READ_ITEM_RESPONSES),
		new CourseOperationContract("/manager/course-runs/{runId}/sessions/{sessionId}", "patch", MUTATE_ITEM_RESPONSES)
	);
	private static final List<CourseOperationContract> COURSE_CATALOG_OPERATION_CONTRACTS = List.of(
		new CourseOperationContract("/courses", "get", Set.of("200", "400", "401")),
		new CourseOperationContract("/courses/{courseId}", "get", Set.of("200", "400", "401", "404")),
		new CourseOperationContract("/course-runs", "get", Set.of("200", "400", "401")),
		new CourseOperationContract("/course-runs/{runId}", "get", Set.of("200", "400", "401", "404"))
	);
	private static final List<CourseOperationContract> COURSE_ORDER_OPERATION_CONTRACTS = List.of(
		new CourseOperationContract("/members/me/course-orders", "post",
			Set.of("200", "201", "400", "401", "403", "404", "409")),
		new CourseOperationContract("/members/me/course-orders/{orderId}", "get",
			Set.of("200", "400", "401", "403", "404")),
		new CourseOperationContract("/reception/members/{memberId}/course-orders", "post",
			Set.of("200", "201", "400", "401", "403", "404", "409")),
		new CourseOperationContract("/reception/members/{memberId}/course-orders/{orderId}", "get",
			Set.of("200", "400", "401", "403", "404"))
	);
	private static final List<CatalogParameterContract> COURSE_CATALOG_PARAMETER_CONTRACTS = List.of(
		new CatalogParameterContract("/courses", "page", "query", false, Set.of("integer"), "int32",
			"0", "0", null, Set.of()),
		new CatalogParameterContract("/courses", "size", "query", false, Set.of("integer"), "int32",
			"20", "1", "100", Set.of()),
		new CatalogParameterContract("/courses/{courseId}", "courseId", "path", true, Set.of("string"),
			"uuid", null, null, null, Set.of()),
		new CatalogParameterContract("/course-runs", "page", "query", false, Set.of("integer"), "int32",
			"0", "0", null, Set.of()),
		new CatalogParameterContract("/course-runs", "size", "query", false, Set.of("integer"), "int32",
			"20", "1", "100", Set.of()),
		new CatalogParameterContract("/course-runs/{runId}", "runId", "path", true, Set.of("string"),
			"uuid", null, null, null, Set.of())
	);
	private static final List<CatalogSchemaContract> COURSE_CATALOG_SCHEMA_CONTRACTS = List.of(
		new CatalogSchemaContract("CatalogCourseResponse", List.of(
			new CatalogPropertyContract("id", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("code", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("name", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("description", Set.of("string", "null"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("disciplineId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("disciplineName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("imageUrl", Set.of("string", "null"), null, null, null, Set.of(), null)
		)),
		new CatalogSchemaContract("CatalogCoursePageResponse", List.of(
			new CatalogPropertyContract("content", Set.of("array"), null, null, null, Set.of(),
				"CatalogCourseResponse"),
			new CatalogPropertyContract("page", Set.of("integer"), "int32", "0", null, Set.of(), null),
			new CatalogPropertyContract("size", Set.of("integer"), "int32", "1", "100", Set.of(), null),
			new CatalogPropertyContract("totalElements", Set.of("integer"), "int64", "0", null, Set.of(), null),
			new CatalogPropertyContract("totalPages", Set.of("integer"), "int32", "0", null, Set.of(), null)
		)),
		new CatalogSchemaContract("CatalogCourseRunResponse", List.of(
			new CatalogPropertyContract("id", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("runCode", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("courseId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("courseCode", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("courseName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("primaryCoachId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("primaryCoachName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("tuitionVnd", Set.of("integer"), null, "1", "9999999999999999999",
				Set.of(), null),
			new CatalogPropertyContract("capacity", Set.of("integer"), "int32", "1", null, Set.of(), null),
			new CatalogPropertyContract("registrationOpensAt", Set.of("string"), "date-time", null, null,
				Set.of(), null),
			new CatalogPropertyContract("registrationClosesAt", Set.of("string"), "date-time", null, null,
				Set.of(), null),
			new CatalogPropertyContract("registrationStatus", Set.of("string"), null, null, null,
				Set.of("UPCOMING", "OPEN", "CLOSED"), null),
			new CatalogPropertyContract("plannedStartAt", Set.of("string"), "date-time", null, null, Set.of(), null),
			new CatalogPropertyContract("plannedEndAt", Set.of("string"), "date-time", null, null, Set.of(), null),
			new CatalogPropertyContract("totalUnits", Set.of("integer"), "int32", "1", null, Set.of(), null),
			new CatalogPropertyContract("status", Set.of("string"), null, null, null,
				Set.of("OPEN_FOR_REGISTRATION"), null)
		)),
		new CatalogSchemaContract("CatalogCourseRunPageResponse", List.of(
			new CatalogPropertyContract("content", Set.of("array"), null, null, null, Set.of(),
				"CatalogCourseRunResponse"),
			new CatalogPropertyContract("page", Set.of("integer"), "int32", "0", null, Set.of(), null),
			new CatalogPropertyContract("size", Set.of("integer"), "int32", "1", "100", Set.of(), null),
			new CatalogPropertyContract("totalElements", Set.of("integer"), "int64", "0", null, Set.of(), null),
			new CatalogPropertyContract("totalPages", Set.of("integer"), "int32", "0", null, Set.of(), null)
		)),
		new CatalogSchemaContract("CatalogCourseSessionResponse", List.of(
			new CatalogPropertyContract("id", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("unitNumber", Set.of("integer"), "int32", "1", null, Set.of(), null),
			new CatalogPropertyContract("roomId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("roomName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("coachId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("coachName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("startAt", Set.of("string"), "date-time", null, null, Set.of(), null),
			new CatalogPropertyContract("endAt", Set.of("string"), "date-time", null, null, Set.of(), null),
			new CatalogPropertyContract("originalStartAt", Set.of("string"), "date-time", null, null,
				Set.of(), null),
			new CatalogPropertyContract("originalEndAt", Set.of("string"), "date-time", null, null,
				Set.of(), null),
			new CatalogPropertyContract("allocationVnd", Set.of("integer"), null, "1", "9999999999999999999",
				Set.of(), null),
			new CatalogPropertyContract("status", Set.of("string"), null, null, null,
				Set.of("SCHEDULED", "CANCELLED_PENDING_MAKEUP", "MAKEUP_SCHEDULED", "COMPLETED", "UNRESOLVED",
					"RESOLVED_BY_REFUND"), null)
		)),
		new CatalogSchemaContract("CatalogCourseRunDetailResponse", List.of(
			new CatalogPropertyContract("id", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("runCode", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("courseId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("courseCode", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("courseName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("primaryCoachId", Set.of("string"), "uuid", null, null, Set.of(), null),
			new CatalogPropertyContract("primaryCoachName", Set.of("string"), null, null, null, Set.of(), null),
			new CatalogPropertyContract("tuitionVnd", Set.of("integer"), null, "1", "9999999999999999999",
				Set.of(), null),
			new CatalogPropertyContract("capacity", Set.of("integer"), "int32", "1", null, Set.of(), null),
			new CatalogPropertyContract("registrationOpensAt", Set.of("string"), "date-time", null, null,
				Set.of(), null),
			new CatalogPropertyContract("registrationClosesAt", Set.of("string"), "date-time", null, null,
				Set.of(), null),
			new CatalogPropertyContract("registrationStatus", Set.of("string"), null, null, null,
				Set.of("UPCOMING", "OPEN", "CLOSED"), null),
			new CatalogPropertyContract("plannedStartAt", Set.of("string"), "date-time", null, null, Set.of(), null),
			new CatalogPropertyContract("plannedEndAt", Set.of("string"), "date-time", null, null, Set.of(), null),
			new CatalogPropertyContract("totalUnits", Set.of("integer"), "int32", "1", null, Set.of(), null),
			new CatalogPropertyContract("status", Set.of("string"), null, null, null,
				Set.of("OPEN_FOR_REGISTRATION"), null),
			new CatalogPropertyContract("sessions", Set.of("array"), null, null, null, Set.of(),
				"CatalogCourseSessionResponse")
		))
	);

	private static final Set<String> EXPECTED_OPERATIONS = Set.of(
		"POST /auth/login",
		"POST /auth/refresh",
		"POST /auth/logout",
		"POST /auth/change-password",
		"POST /auth/register",
		"POST /auth/email-verification/verify",
		"POST /auth/email-verification/resend",
		"POST /auth/email-verification/change-email",
		"POST /auth/password-reset/request",
		"POST /auth/password-reset/verify",
		"POST /auth/password-reset/confirm",
		"GET /reception/email-verification/pending-accounts",
		"POST /reception/email-verification/manual-activation-requests",
		"GET /reception/email-verification/manual-activation-requests",
		"GET /reception/email-verification/manual-activation-requests/{requestId}",
		"GET /manager/email-verification/manual-activation-requests",
		"GET /manager/email-verification/manual-activation-requests/{requestId}",
		"POST /manager/email-verification/manual-activation-requests/{requestId}/approve",
		"POST /manager/email-verification/manual-activation-requests/{requestId}/reject",
		"GET /manager/members",
		"GET /manager/members/{id}",
		"PATCH /manager/members/{id}",
		"PATCH /manager/members/{memberAccountId}/status",
		"POST /manager/staff-accounts",
		"GET /manager/staff-accounts",
		"GET /manager/staff-accounts/{accountId}",
		"PATCH /manager/staff-accounts/{accountId}",
		"PATCH /manager/staff-accounts/{accountId}/status",
		"POST /manager/staff-accounts/{accountId}/reset-password",
		"GET /members/me/profile",
		"PATCH /members/me/profile",
		"POST /members/me/profile/image",
		"POST /reception/members/{memberId}/membership-orders/{orderId}/payments/sepay",
		"GET /staff/me/profile",
		"PATCH /staff/me/profile",
		"POST /staff/me/profile/image",
		"POST /staff/me/email-change",
		"POST /staff/me/email-change/verify",
		"POST /staff/me/email-change/resend",
		"DELETE /staff/me/email-change",
		"GET /accounts/me/email-change",
		"POST /accounts/me/email-change",
		"PUT /accounts/me/email-change",
		"POST /accounts/me/email-change/verify",
		"POST /accounts/me/email-change/resend",
		"DELETE /accounts/me/email-change",
		"GET /membership-offers",
		"GET /membership-offers/{offerId}",
		"GET /courses",
		"GET /courses/{courseId}",
		"GET /course-runs",
		"GET /course-runs/{runId}",
		"GET /manager/membership-offers",
		"GET /manager/membership-offers/{offerId}",
		"POST /manager/membership-offers",
		"PATCH /manager/membership-offers/{id}",
		"GET /manager/membership-benefits",
		"GET /manager/membership-plans",
		"GET /manager/membership-plans/{planCode}",
		"POST /manager/membership-plans",
		"PATCH /manager/membership-plans/{planCode}",
		"GET /members/me/membership-termination-requests",
		"POST /members/me/membership-termination-requests",
		"DELETE /members/me/membership-termination-requests/{requestId}",
		"GET /reception/members/{memberCode}/membership-termination-requests",
		"POST /reception/members/{memberCode}/membership-termination-requests",
		"GET /manager/membership-termination-requests",
		"POST /manager/membership-termination-requests/{requestId}/approve",
		"POST /manager/membership-termination-requests/{requestId}/reject",
		"POST /manager/members/{memberAccountId}/membership-termination-requests",
		"GET /members/me/membership-freeze-requests",
		"POST /members/me/membership-freeze-requests",
		"DELETE /members/me/membership-freeze-requests/{requestId}",
		"POST /members/me/membership-freeze-requests/{requestId}/end-early",
		"GET /reception/members/{memberCode}/membership-freeze-requests",
		"POST /reception/members/{memberCode}/membership-freeze-requests",
		"POST /reception/membership-freeze-requests/{requestId}/end-early",
		"GET /manager/membership-freeze-requests",
		"POST /manager/membership-freeze-requests/{requestId}/approve",
		"POST /manager/membership-freeze-requests/{requestId}/reject",
		"POST /reception/membership-freeze-requests/{requestId}/cash-payment",
		"GET /reception/refund-requests",
		"GET /reception/members/{memberCode}/refund-eligible-sources",
		"POST /reception/members/{memberCode}/refund-requests",
		"POST /reception/refund-requests/{requestId}/cash-disbursement",
		"GET /manager/refund-requests",
		"POST /manager/refund-requests/{requestId}/first-approval",
		"POST /manager/refund-requests/{requestId}/final-approval",
		"POST /manager/refund-requests/{requestId}/reject",
		"POST /members/me/membership-orders",
		"GET /members/me/membership-orders/pending",
		"POST /reception/members/{memberId}/membership-orders",
		"GET /reception/members/{memberId}/membership-orders/pending",
		"POST /members/me/course-orders",
		"GET /members/me/course-orders/{orderId}",
		"POST /reception/members/{memberId}/course-orders",
		"GET /reception/members/{memberId}/course-orders/{orderId}",
		"POST /reception/members/{memberId}/cash-payments",
		"POST /reception/membership-orders/{orderId}/cash",
		"POST /payments/{paymentId}/reconcile",
		"PATCH /membership-orders/{orderId}/cancel",
		"GET /payments/reconciliation-queue",
		"GET /members/me/receipts",
		"GET /reception/members/{memberId}/receipts",
		"GET /receipts/{receiptId}",
		"GET /payments/{paymentId}/result",
		"GET /members/me/memberships",
		"GET /reception/members/{memberCode}/memberships",
		"GET /manager/members/{memberAccountId}/memberships",
		"GET /manager/reports/membership-revenue",
		"GET /manager/dashboard",
		"GET /manager/reports/members",
		"GET /manager/reports/bookings",
		"GET /manager/reports/attendance",
		"GET /manager/reports/center-visits",
		"GET /manager/reports/class-utilization",
		"POST /members/me/membership-orders/{orderId}/payments/sepay",
		"POST /members/me/membership-orders/{orderId}/bank-transfer",
		"POST /payments/sepay/webhook",
		"POST /reception/members/{memberId}/reset-password",
		"POST /reception/members",
		"GET /reception/members/search",
		"GET /reception/members/{memberId}/profile",
		"PATCH /reception/members/{memberId}/profile",
		"POST /reception/members/{memberId}/profile/image",
		"POST /reception/center-visits",
		"GET /members/me/center-visits/current",
		"PATCH /members/me/center-visits/current/checkout",
		"PATCH /reception/members/{memberAccountId}/center-visits/current/checkout",
		"GET /reception/center-visits/open",
		"POST /members/class-sessions/{sessionId}/bookings",
		"POST /members/class-sessions/{sessionId}/waitlist",
		"DELETE /members/class-sessions/{sessionId}/waitlist",
		"GET /members/me/waitlists",
		"POST /reception/members/{memberId}/class-sessions/{sessionId}/waitlist",
		"DELETE /reception/members/{memberId}/class-sessions/{sessionId}/waitlist",
		"GET /reception/class-sessions/{sessionId}/waitlist",
		"GET /manager/class-sessions/{sessionId}/waitlist",
		"GET /members/me/bookings",
		"PATCH /members/me/bookings/{bookingId}/cancel",
		"POST /reception/members/{memberId}/bookings",
		"GET /reception/members/{memberId}/bookings",
		"PATCH /reception/members/{memberId}/bookings/{bookingId}/cancel",
		"GET /reception/class-sessions",
		"GET /manager/disciplines",
		"POST /manager/disciplines",
		"GET /manager/disciplines/{disciplineId}",
		"PATCH /manager/disciplines/{disciplineId}",
		"GET /manager/courses",
		"POST /manager/courses",
		"GET /manager/courses/{courseId}",
		"PATCH /manager/courses/{courseId}",
		"POST /manager/courses/{courseId}/publish",
		"POST /manager/courses/{courseId}/deactivate",
		"POST /manager/courses/{courseId}/archive",
		"GET /manager/course-runs",
		"POST /manager/course-runs",
		"GET /manager/course-runs/{runId}",
		"PATCH /manager/course-runs/{runId}",
		"POST /manager/course-runs/{runId}/open-registration",
		"POST /manager/course-runs/{runId}/cancel",
		"GET /manager/course-runs/{runId}/sessions",
		"POST /manager/course-runs/{runId}/sessions",
		"GET /manager/course-runs/{runId}/sessions/{sessionId}",
		"PATCH /manager/course-runs/{runId}/sessions/{sessionId}",
		"GET /manager/rooms",
		"GET /manager/rooms/availability",
		"POST /manager/rooms",
		"GET /manager/rooms/{roomId}",
		"PATCH /manager/rooms/{roomId}",
		"GET /manager/rooms/{roomId}/suspensions",
		"POST /manager/rooms/{roomId}/suspensions",
		"PATCH /manager/rooms/{roomId}/suspensions/{suspensionId}",
		"POST /manager/rooms/{roomId}/impact-preview",
		"POST /manager/rooms/{roomId}/impact-apply",
		"GET /manager/classes",
		"GET /manager/classes/types",
		"POST /manager/classes",
		"GET /manager/classes/{classId}",
		"PATCH /manager/classes/{classId}",
		"POST /manager/class-sessions",
		"GET /manager/class-sessions",
		"GET /manager/class-sessions/{id}",
		"GET /manager/class-sessions/{id}/available-coaches",
		"GET /manager/coaches/available",
		"PATCH /manager/class-sessions/{id}/assignment",
		"PATCH /manager/class-sessions/{id}/schedule",
		"PATCH /manager/class-sessions/{id}/cancel",
		"POST /manager/class-sessions/cancellation-preview",
		"POST /manager/class-sessions/bulk-cancel",
		"POST /coach/leave-requests",
		"GET /coach/me/leave-requests",
		"DELETE /coach/leave-requests/{requestId}",
		"PATCH /coach/leave-requests/{requestId}/cancel",
		"GET /manager/coach-leave-requests",
		"PATCH /manager/coach-leave-requests/{requestId}/approve",
		"PATCH /manager/coach-leave-requests/{requestId}/reject",
		"POST /manager/recurring-schedules",
		"GET /members/class-sessions",
		"GET /members/pt-sessions",
		"GET /reception/pt-sessions",
		"GET /manager/pt-sessions",
		"GET /coach/pt-sessions",
		"GET /coach/class-sessions",
		"GET /coach/class-sessions/{id}",
		"GET /coach/class-sessions/{sessionId}/attendance",
		"PATCH /coach/class-sessions/{sessionId}/attendance/{attendanceId}",
		"GET /coach/members",
		"GET /coach/members/{memberId}",
		"GET /coach/members/{memberId}/training-plans",
		"POST /coach/members/{memberId}/training-plans",
		"PUT /coach/members/{memberId}/training-plans/{planId}",
		"GET /coach/members/{memberId}/training-plans/{planId}/versions",
		"POST /coach/members/{memberId}/training-plans/{planId}/archive",
		"POST /coach/members/{memberId}/training-results",
		"GET /coach/members/{memberId}/training-results",
		"PUT /coach/members/{memberId}/training-results/{resultId}",
		"POST /coach/members/{memberId}/feedback",
		"GET /coach/members/{memberId}/feedback",
		"PUT /coach/members/{memberId}/feedback/{feedbackId}",
		"POST /coach/class-sessions/{sessionId}/members/{memberId}/training-results",
		"GET /coach/class-sessions/{sessionId}/members/{memberId}/training-results",
		"PUT /coach/class-sessions/{sessionId}/members/{memberId}/training-results/{resultId}",
		"POST /coach/class-sessions/{sessionId}/members/{memberId}/feedback",
		"GET /coach/class-sessions/{sessionId}/members/{memberId}/feedback",
		"PUT /coach/class-sessions/{sessionId}/members/{memberId}/feedback/{feedbackId}",
		"GET /members/me/training-results",
		"GET /members/me/feedback",
		"GET /members/me/training-plans",
		"GET /members/me/coach-assignment",
		"GET /notifications",
		"PATCH /notifications/{notificationId}/read",
		"PATCH /notifications/read-all",
		"GET /manager/audit-events",
		"GET /manager/coach-assignments/candidates",
		"GET /manager/coaches/assignment-workloads",
		"POST /manager/members/{memberId}/coach-assignment",
		"GET /manager/members/{memberId}/coach-assignment",
		"GET /manager/members/{memberId}/coach-assignment/impact",
		"GET /manager/members/{memberId}/coach-assignments",
		"PATCH /manager/members/{memberId}/coach-assignment/end"
	);

	private static final Set<String> INTENTIONAL_NO_CONTENT = Set.of(
		"POST /auth/logout",
		"POST /auth/change-password",
		"POST /auth/password-reset/confirm",
		"DELETE /staff/me/email-change",
		"DELETE /accounts/me/email-change",
		"DELETE /members/me/membership-termination-requests/{requestId}",
		"DELETE /members/me/membership-freeze-requests/{requestId}",
		"POST /manager/staff-accounts/{accountId}/reset-password",
		"POST /reception/members/{memberId}/reset-password",
		"GET /members/me/membership-orders/pending",
		"GET /reception/members/{memberId}/membership-orders/pending",
		"GET /manager/members/{memberId}/coach-assignment",
		"GET /members/me/coach-assignment",
		"GET /coach/class-sessions/{sessionId}/members/{memberId}/training-results",
		"GET /coach/class-sessions/{sessionId}/members/{memberId}/feedback"
	);

	private static final Set<String> HTTP_METHODS = Set.of(
		"get", "post", "put", "patch", "delete", "head", "options", "trace"
	);
	private static final String CONFLICT_ONLY_OPERATION =
		"POST /reception/membership-orders/{orderId}/cash";

	@Autowired
	MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void controllerSuccessResponsesHaveConcreteJavaTypes() throws Exception {
		ClassPathScanningCandidateComponentProvider scanner = controllerScanner();
		List<String> wildcardMethods = new ArrayList<>();
		for (var candidate : scanner.findCandidateComponents("com.scms.backend")) {
			Class<?> controller = Class.forName(candidate.getBeanClassName());
			for (var method : controller.getDeclaredMethods()) {
				if (method.getGenericReturnType() instanceof ParameterizedType type
						&& type.getRawType().getTypeName().equals("org.springframework.http.ResponseEntity")
						&& type.getActualTypeArguments()[0] instanceof WildcardType) {
					wildcardMethods.add(controller.getSimpleName() + "." + method.getName());
				}
			}
		}
		assertThat(wildcardMethods).isEmpty();
	}

	@Test
	void generatedContractContainsEveryRouteAndConcreteSuccessSchema() throws Exception {
		JsonNode document = generatedDocument();
		Set<String> actualOperations = operations(document);

		assertThat(actualOperations).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS);

		document.path("paths").properties().forEach(pathEntry ->
			pathEntry.getValue().properties().forEach(operationEntry -> {
				if (!HTTP_METHODS.contains(operationEntry.getKey())) return;
				String operation = operationEntry.getKey().toUpperCase() + " " + pathEntry.getKey();
				List<java.util.Map.Entry<String, JsonNode>> successResponses = operationEntry.getValue()
					.path("responses").properties().stream()
					.filter(responseEntry -> responseEntry.getKey().matches("2\\d\\d"))
					.toList();
				if (operation.equals(CONFLICT_ONLY_OPERATION)) {
					assertThat(successResponses).as(operation).isEmpty();
				}
				else {
					assertThat(successResponses).as(operation).isNotEmpty();
				}
				successResponses.forEach(responseEntry -> {
					if (responseEntry.getKey().equals("204")) {
						assertThat(INTENTIONAL_NO_CONTENT).as(operation + " 204").contains(operation);
						return;
					}
					assertConcreteSchema(responseEntry.getValue(), operation + " " + responseEntry.getKey());
				});
			}));
		INTENTIONAL_NO_CONTENT.forEach(operation -> {
			int separator = operation.indexOf(' ');
			String method = operation.substring(0, separator).toLowerCase();
			String path = operation.substring(separator + 1);
			assertThat(response(document, path, method, "204").isMissingNode()).as(operation).isFalse();
		});
	}

	@Test
	void generatedCourseContractDocumentsManagerSecurityValidationEnumsAndProblems() throws Exception {
		JsonNode document = generatedDocument();
		JsonNode create = operation(document, "/manager/courses", "post");
		assertThat(create.path("description").asText()).isEqualTo("Requires MANAGER role.");
		assertThat(create.at("/responses/400/$ref").asText()).isEqualTo("#/components/responses/BadRequest");
		assertThat(create.at("/responses/409/$ref").asText()).isEqualTo("#/components/responses/Conflict");
		JsonNode request = document.at("/components/schemas/CourseCreateRequest");
		assertThat(request.path("required").toString()).contains("code", "name", "disciplineId");
		assertThat(request.at("/properties/code/maxLength").asInt()).isEqualTo(50);
		assertThat(request.at("/properties/description/maxLength").asInt()).isEqualTo(2000);
		assertThat(request.at("/properties/imageUrl/maxLength").asInt()).isEqualTo(500);
		JsonNode patch = document.at("/components/schemas/CoursePatchRequest");
		assertThat(patch.at("/properties/description/maxLength").asInt()).isEqualTo(2000);
		assertThat(patch.at("/properties/imageUrl/maxLength").asInt()).isEqualTo(500);
		assertThat(document.at("/components/schemas/CourseResponse/properties/status/enum").toString())
			.contains("DRAFT", "PUBLISHED", "INACTIVE", "ARCHIVED");
		assertThat(document.at("/components/schemas/CourseRunResponse/properties/status/enum").toString())
			.contains("OPEN_FOR_REGISTRATION", "SETTLEMENT_PENDING", "CANCELLED");
		assertThat(document.at("/components/schemas/CourseResponse/properties/createdAt/format").asText())
			.isEqualTo("date-time");
	}

	@Test
	void generatedCourseCatalogIsPublicAndDocumentsOnlyVisibleRunState() throws Exception {
		JsonNode document = generatedDocument();
		for (String path : List.of("/courses", "/courses/{courseId}", "/course-runs", "/course-runs/{runId}")) {
			JsonNode operation = operation(document, path, "get");
			assertThat(operation.path("security").isArray()).as(path).isTrue();
			assertThat(operation.path("security")).as(path).isEmpty();
			assertThat(operation.path("description").asText()).contains("Public course catalog");
			assertThat(operation.path("responses").has("403")).as(path).isFalse();
		}
		assertThat(document.at("/components/schemas/CatalogCourseRunResponse/properties/status/enum").toString())
			.isEqualTo("[\"OPEN_FOR_REGISTRATION\"]");
		assertThat(document.at("/components/schemas/CatalogCourseRunResponse/properties/registrationStatus/enum")
			.toString())
			.isEqualTo("[\"UPCOMING\",\"OPEN\",\"CLOSED\"]");
		JsonNode detail = document.at("/components/schemas/CatalogCourseRunDetailResponse");
		assertThat(detail.at("/properties/sessions/items/$ref").asText())
			.isEqualTo("#/components/schemas/CatalogCourseSessionResponse");
		assertThat(detail.toString()).doesNotContain("capacityRemaining", "policyVersionSnapshot", "version");
	}

	@Test
	void staticAndGeneratedCourseCatalogContractsHaveNormalizedParity() throws Exception {
		JsonNode generated = generatedDocument();
		JsonNode documented = new ObjectMapper(new YAMLFactory()).readTree(
			getClass().getResourceAsStream("/openapi/course-catalog.yaml"));

		for (CourseOperationContract contract : COURSE_CATALOG_OPERATION_CONTRACTS) {
			String label = contract.method().toUpperCase() + " " + contract.path();
			assertThat(responseStatuses(generated, contract)).as("generated " + label)
				.containsExactlyInAnyOrderElementsOf(contract.statuses());
			assertThat(responseStatuses(documented, contract)).as("static " + label)
				.containsExactlyInAnyOrderElementsOf(contract.statuses());
		}
		assertExpectedCatalogContract(generated, "generated");
		assertExpectedCatalogContract(documented, "static");
		assertThat(normalizedCatalogContract(documented)).isEqualTo(normalizedCatalogContract(generated));
	}

	@Test
	void staticAndGeneratedCourseOrderContractsHaveParity() throws Exception {
		JsonNode generated = generatedDocument();
		JsonNode documented = new ObjectMapper(new YAMLFactory()).readTree(
			getClass().getResourceAsStream("/openapi/course-orders.yaml"));

		for (CourseOperationContract contract : COURSE_ORDER_OPERATION_CONTRACTS) {
			String label = contract.method().toUpperCase() + " " + contract.path();
			assertThat(responseStatuses(generated, contract)).as("generated " + label)
				.containsExactlyInAnyOrderElementsOf(contract.statuses());
			assertThat(responseStatuses(documented, contract)).as("static " + label)
				.containsExactlyInAnyOrderElementsOf(contract.statuses());
			if ("post".equals(contract.method())) {
				assertThat(operation(generated, contract.path(), contract.method()).path("parameters"))
					.anySatisfy(parameter -> {
						assertThat(parameter.path("name").asText()).isEqualTo("Idempotency-Key");
						assertThat(parameter.path("required").asBoolean()).isTrue();
					});
			}
		}
		assertThat(documented.at("/components/schemas/CourseOrderResponse/required").toString())
			.contains("orderId", "orderReference", "memberId", "courseRunId", "tuitionVnd", "currencyCode",
				"orderStatus", "reservationStatus", "expiresAt", "createdAt");
		assertThat(documented.at("/components/schemas/CourseOrderResponse/properties/orderStatus/enum").toString())
			.isEqualTo("[\"PENDING_PAYMENT\",\"EXPIRED\"]");
		assertThat(documented.at("/components/schemas/CourseOrderResponse/properties/reservationStatus/enum").toString())
			.isEqualTo("[\"HELD\",\"EXPIRED\"]");
	}

	@Test
	void staticAndGeneratedCourseContractsHaveTableDrivenResponseAndProblemParity() throws Exception {
		JsonNode generated = generatedDocument();
		JsonNode documented = new ObjectMapper(new YAMLFactory()).readTree(
			getClass().getResourceAsStream("/openapi/course-manager.yaml"));

		for (CourseOperationContract contract : COURSE_OPERATION_CONTRACTS) {
			String label = contract.method().toUpperCase() + " " + contract.path();
			assertThat(responseStatuses(generated, contract)).as("generated " + label)
				.containsExactlyInAnyOrderElementsOf(contract.statuses());
			assertThat(responseStatuses(documented, contract)).as("static " + label)
				.containsExactlyInAnyOrderElementsOf(contract.statuses());
			contract.statuses().stream().filter(status -> !status.startsWith("2")).forEach(status -> {
				assertProblemResponse(generated, contract, status, "generated " + label,
					"401".equals(status) ? "UnauthorizedProblem" : "ApiProblem");
				assertProblemResponse(documented, contract, status, "static " + label, "CourseProblem");
			});
		}

		assertThat(documented.at("/components/schemas/CourseProblem/required").toString())
			.contains("title", "status", "detail", "instance", "code");
		assertThat(documented.at("/components/responses/Unauthorized/description").asText())
			.contains("UNAUTHENTICATED", "INVALID_TOKEN");
		assertThat(documented.at("/components/responses/Forbidden/description").asText()).contains("FORBIDDEN");
		assertThat(generated.at("/components/responses/Unauthorized/description").asText())
			.contains("UNAUTHENTICATED", "INVALID_TOKEN");
		assertThat(generated.at("/components/responses/Forbidden/description").asText()).contains("FORBIDDEN");
		assertThat(documented.at("/components/schemas/CourseProblem/properties/code/enum").values())
			.toIterable().extracting(JsonNode::asText)
			.containsExactlyInAnyOrderElementsOf(COURSE_PROBLEM_CODES);
	}

	@Test
	void generatedUnauthorizedComponentEnumeratesEveryActualStable401Code() throws Exception {
		JsonNode document = generatedDocument();

		assertThat(response(document, "/auth/login", "post", "401").path("$ref").asText())
			.isEqualTo("#/components/responses/Unauthorized");
		assertThat(response(document, "/auth/refresh", "post", "401").path("$ref").asText())
			.isEqualTo("#/components/responses/Unauthorized");
		assertThat(document.at("/components/responses/Unauthorized/content/application~1problem+json/schema/$ref")
			.asText()).isEqualTo("#/components/schemas/UnauthorizedProblem");
		assertThat(document.at("/components/schemas/UnauthorizedProblem/properties/code/enum").values())
			.toIterable().extracting(JsonNode::asText)
			.containsExactlyInAnyOrderElementsOf(UNAUTHORIZED_PROBLEM_CODES);
	}

	@Test
	void sepayWebhookDocumentsAnonymousApiKeyHeaderAuthentication() throws Exception {
		JsonNode document = generatedDocument();
		JsonNode webhook = operation(document, "/payments/sepay/webhook", "post");

		assertThat(webhook.path("security").isArray()).isTrue();
		assertThat(webhook.path("security")).isEmpty();
		assertThat(webhook.path("description").asText()).contains("API key", "header");
		JsonNode authorization = null;
		for (JsonNode parameter : webhook.path("parameters")) {
			if ("Authorization".equals(parameter.path("name").asText())) authorization = parameter;
		}
		assertThat(authorization).isNotNull();
		assertThat(authorization.path("in").asText()).isEqualTo("header");
		assertThat(authorization.path("description").asText()).contains("Apikey <credential>");
		assertThat(webhook.at("/responses/200/description").asText()).contains("durably handled");
		assertThat(webhook.at("/responses/400/description").asText()).contains("Malformed");
		assertThat(webhook.at("/responses/401/description").asText()).contains("API-key credentials");
		assertThat(webhook.at("/responses/500/description").asText()).contains("Transient");
		for (String status : List.of("400", "401", "500")) {
			assertThat(webhook.at("/responses/" + status
				+ "/content/application~1problem+json/schema/$ref").asText())
				.as("webhook " + status)
				.isEqualTo("#/components/schemas/ApiProblem");
		}
	}

	@Test
	void sepayWebhookAcknowledgementSchemaRequiresSuccessProcessedAndOutcome() throws Exception {
		JsonNode document = generatedDocument();
		JsonNode schema = response(document, "/payments/sepay/webhook", "post", "200")
			.path("content").elements().next().path("schema");
		if (schema.has("$ref")) schema = document.at(schema.path("$ref").asText().substring(1));

		List<String> requiredFields = new ArrayList<>();
		schema.path("required").forEach(field -> requiredFields.add(field.asText()));
		assertThat(requiredFields).contains("success", "processed", "outcome");
	}

	@Test
	void sepayCreateOperationsDocumentConfigurationInvalidServiceUnavailable() throws Exception {
		JsonNode document = generatedDocument();
		for (String path : List.of(
			"/members/me/membership-orders/{orderId}/payments/sepay",
			"/reception/members/{memberId}/membership-orders/{orderId}/payments/sepay")) {
			JsonNode unavailable = response(document, path, "post", "503");
			assertThat(unavailable.path("description").asText().toLowerCase())
				.as(path)
				.contains("configuration", "invalid");
			assertThat(unavailable.at("/content/application~1problem+json/schema/$ref").asText())
				.as(path)
				.isEqualTo("#/components/schemas/ApiProblem");
		}
	}

	@Test
	void generatedContractDocumentsSecuritySchemasAndDynamicStatuses() throws Exception {
		JsonNode document = generatedDocument();

		assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();
		for (String publicOperation : List.of(
			"/paths/~1auth~1login/post", "/paths/~1auth~1refresh/post",
			"/paths/~1auth~1logout/post", "/paths/~1auth~1register/post",
			"/paths/~1auth~1email-verification~1verify/post",
			"/paths/~1auth~1email-verification~1resend/post",
			"/paths/~1auth~1email-verification~1change-email/post",
			"/paths/~1membership-offers/get", "/paths/~1membership-offers~1{offerId}/get",
			"/paths/~1courses/get", "/paths/~1courses~1{courseId}/get",
			"/paths/~1course-runs/get", "/paths/~1course-runs~1{runId}/get",
			"/paths/~1payments~1sepay~1webhook/post")) {
			assertThat(document.at(publicOperation + "/security").isArray()).as(publicOperation).isTrue();
			assertThat(document.at(publicOperation + "/security")).as(publicOperation).isEmpty();
		}
		assertThat(document.at("/paths/~1members~1me~1profile/get/security").isMissingNode()).isTrue();
		assertThat(document.at("/paths/~1members~1me~1profile/get/description").asText())
			.contains("MEMBER", "ownership");
		assertThat(document.at("/paths/~1manager~1staff-accounts/get/description").asText())
			.contains("MANAGER");
		assertThat(document.at("/paths/~1reception~1members~1{memberId}~1receipts/get/description").asText())
			.contains("MANAGER", "RECEPTIONIST");

		for (String status : List.of("400", "401", "403")) {
			JsonNode response = document.at("/paths/~1members~1me~1profile/get/responses/" + status);
			assertThat(response.isMissingNode()).as(status).isFalse();
			assertThat(response.path("$ref").asText()).startsWith("#/components/responses/");
		}
		assertThat(document.at("/components/responses/BadRequest/content/application~1problem+json")
			.isMissingNode()).isFalse();
		assertThat(document.at("/components/schemas/ApiProblem/required").toString())
			.contains("title", "status", "detail", "instance", "code");
		assertThat(document.at("/paths/~1auth~1login/post/responses/403").isMissingNode()).isTrue();
		assertThat(document.at("/paths/~1manager~1staff-accounts/post/responses/409").isMissingNode()).isFalse();
		for (String orderCreationPath : List.of(
			"/members/me/membership-orders",
			"/reception/members/{memberId}/membership-orders",
			"/reception/members/{memberId}/cash-payments")) {
			assertThat(response(document, orderCreationPath, "post", "409").path("$ref").asText())
				.as(orderCreationPath)
				.isEqualTo("#/components/responses/MembershipOrderConflict");
		}
		assertThat(document.at("/components/responses/MembershipOrderConflict/description").asText())
			.contains("MEMBERSHIP_OFFER_NOT_AVAILABLE");
		JsonNode cashParameters = operation(document, "/reception/members/{memberId}/cash-payments", "post")
			.path("parameters");
		List<JsonNode> cashParameterList = new ArrayList<>();
		cashParameters.forEach(cashParameterList::add);
		assertThat(cashParameterList).anyMatch(parameter -> "Idempotency-Key".equals(parameter.path("name").asText())
			&& "header".equals(parameter.path("in").asText()) && !parameter.path("required").asBoolean());

		assertProblemReference(document, "/reception/members/search", "get", "404");
		assertProblemReference(document, "/coach/class-sessions/{sessionId}/attendance", "get", "409");
		assertThat(response(document, "/reception/center-visits", "post", "200").isMissingNode()).isFalse();
		assertConcreteSchema(response(document, "/reception/center-visits", "post", "201"),
			"POST /reception/center-visits 201");
		for (String pendingPath : List.of(
			"/members/me/membership-orders/pending",
			"/reception/members/{memberId}/membership-orders/pending")) {
			assertThat(response(document, pendingPath, "get", "204").isMissingNode()).as(pendingPath).isFalse();
		}
		JsonNode deprecatedResponses = operation(document,
			"/reception/membership-orders/{orderId}/cash", "post").path("responses");
		assertThat(deprecatedResponses.path("409").path("$ref").asText())
			.isEqualTo("#/components/responses/Conflict");
		assertThat(deprecatedResponses.properties().stream().map(java.util.Map.Entry::getKey))
			.noneMatch(status -> status.matches("2\\d\\d"));

		assertThat(document.at("/components/schemas/BookingResponse/properties/status/enum")).isNotEmpty();
		assertThat(document.at("/paths/~1manager~1classes~1types/get/description").asText())
			.contains("MANAGER");
		assertThat(document.at("/components/schemas/SportClassResponse/properties/classType/enum")
			.toString()).isEqualTo("[\"GROUP\",\"YOGA\",\"PT_1_1\"]");
		assertThat(document.at("/paths/~1manager~1classes~1{classId}/patch/responses/409/$ref").asText())
			.isEqualTo("#/components/responses/Conflict");
		assertThat(document.at("/components/schemas/ApiProblem/properties/affectedSessions/items/properties/sessionId/format")
			.asText()).isEqualTo("uuid");
	}

	@Test
	void generatedContractDocumentsDisciplineBenefitsAndBothBookingConflictVariants() throws Exception {
		JsonNode document = generatedDocument();
		for (String schema : List.of("DisciplineCreateRequest", "DisciplinePatchRequest", "DisciplineResponse",
			"MemberClassSessionResponse")) {
			JsonNode requiredBenefits = document.at("/components/schemas/" + schema
				+ "/properties/requiredBenefitCodes");
			assertThat(requiredBenefits.path("type").asText()).as(schema).isEqualTo("array");
			assertThat(requiredBenefits.path("items").path("type").asText()).as(schema).isEqualTo("string");
		}
		JsonNode problem = document.at("/components/schemas/ApiProblem/properties");
		assertThat(problem.path("errors").path("type").asText()).isEqualTo("object");
		assertThat(problem.path("errors").path("additionalProperties").path("type").asText()).isEqualTo("array");
		assertThat(problem.path("requiredBenefitCodes").path("type").asText()).isEqualTo("array");
		assertThat(problem.path("missingBenefitCodes").path("type").asText()).isEqualTo("array");
		for (String path : List.of("/members/class-sessions/{sessionId}/bookings",
			"/reception/members/{memberId}/bookings")) {
			assertThat(response(document, path, "post", "409").path("$ref").asText())
				.as(path).isEqualTo("#/components/responses/BookingConflict");
		}
		JsonNode variants = document.at("/components/responses/BookingConflict/content/application~1problem+json/schema/oneOf");
		assertThat(variants).hasSize(2);
		assertThat(variants.get(0).path("$ref").asText())
			.isEqualTo("#/components/schemas/OrdinaryBookingConflictProblem");
		assertThat(variants.get(1).path("$ref").asText())
			.isEqualTo("#/components/schemas/MembershipBenefitRequiredProblem");
		JsonNode ordinaryCode = document.at("/components/schemas/OrdinaryBookingConflictProblem/properties/code");
		assertThat(ordinaryCode.path("type").asText()).isEqualTo("string");
		assertThat(ordinaryCode.at("/not/enum/0").asText()).isEqualTo("MEMBERSHIP_BENEFIT_REQUIRED");
		JsonNode benefitProblem = document.at("/components/schemas/MembershipBenefitRequiredProblem");
		assertThat(benefitProblem.path("required").toString())
			.contains("requiredBenefitCodes", "missingBenefitCodes");
		assertThat(benefitProblem.at("/properties/code/enum/0").asText())
			.isEqualTo("MEMBERSHIP_BENEFIT_REQUIRED");
		assertThat(document.at("/components/schemas/ApiProblem/required").toString())
			.doesNotContain("requiredBenefitCodes", "missingBenefitCodes");
	}

	@Test
	void generatedContractDocumentsStaffMembershipHistory() throws Exception {
		JsonNode document = generatedDocument();
		for (String path : List.of(
			"/reception/members/{memberCode}/memberships",
			"/manager/members/{memberAccountId}/memberships")) {
			JsonNode operation = operation(document, path, "get");
			assertThat(operation.path("description").asText())
				.contains("ACTIVE", "SUSPENDED", "snapshot", "UTC");
			JsonNode content = response(document, path, "get", "200").path("content");
			assertThat(content.properties().stream().map(java.util.Map.Entry::getKey).toList())
				.containsExactly("application/json");
			JsonNode item = content.path("application/json").path("schema").path("items");
			assertThat(item.path("$ref").asText()).isEqualTo("#/components/schemas/StaffMembershipResponse");
		}
		JsonNode schema = document.at("/components/schemas/StaffMembershipResponse");
		assertThat(schema.path("properties").properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("membershipId", "planCode", "offerName", "status", "startsAt", "endsAt",
				"benefitCodes", "benefits");
		assertThat(schema.path("required").toString())
			.contains("membershipId", "planCode", "offerName", "status", "startsAt", "endsAt", "benefitCodes",
				"benefits");
		assertThat(schema.at("/properties/startsAt/format").asText()).isEqualTo("date-time");
		assertThat(schema.at("/properties/endsAt/format").asText()).isEqualTo("date-time");
		assertThat(schema.at("/properties/status/enum").toString())
			.isEqualTo("[\"RESERVED\",\"ACTIVE\",\"EXPIRED\",\"TERMINATED\",\"CANCELLED\"]");
		assertThat(schema.at("/properties/benefitCodes/type").asText()).isEqualTo("array");
		assertThat(schema.at("/properties/benefits/items/$ref").asText())
			.isEqualTo("#/components/schemas/MembershipBenefitResponse");
		JsonNode memberHistory = document.at("/components/schemas/MembershipHistoryResponse/properties");
		assertThat(memberHistory.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("benefitCodes", "benefits");
		assertThat(memberHistory.at("/status/enum").toString()).contains("CANCELLED");
	}

	@Test
	void generatedAndStaticContractsDescribeRefundEligibleSources() throws Exception {
		JsonNode generated = generatedDocument();
		JsonNode generatedItems = response(generated,
			"/reception/members/{memberCode}/refund-eligible-sources", "get", "200")
			.path("content").path("application/json").path("schema").path("items");
		assertThat(generatedItems.path("$ref").asText())
			.isEqualTo("#/components/schemas/RefundEligibleSourceResponse");
		JsonNode generatedSchema = generated.at("/components/schemas/RefundEligibleSourceResponse");
		assertRefundEligibleSourceSchema(generatedSchema);
		assertThat(generatedSchema.at("/properties/paymentReference/type").toString()).contains("null");

		try (var stream = getClass().getResourceAsStream("/openapi/membership-refund.yaml")) {
			JsonNode documented = new ObjectMapper(new YAMLFactory()).readTree(stream);
			JsonNode documentedItems = response(documented,
				"/reception/members/{memberCode}/refund-eligible-sources", "get", "200")
				.path("content").path("application/json").path("schema").path("items");
			assertThat(documentedItems.path("$ref").asText())
				.isEqualTo("#/components/schemas/RefundEligibleSourceResponse");
			JsonNode documentedSchema = documented.at("/components/schemas/RefundEligibleSourceResponse");
			assertRefundEligibleSourceSchema(documentedSchema);
			assertThat(documentedSchema.at("/properties/paymentReference/nullable").asBoolean()).isTrue();
		}
	}

	@Test
	void generatedAndStaticRefundContractsIncludeNullableDisplayOnEveryResponse() throws Exception {
		JsonNode generated = generatedDocument();
		try (var stream = getClass().getResourceAsStream("/openapi/membership-refund.yaml")) {
			JsonNode documented = new ObjectMapper(new YAMLFactory()).readTree(stream);
			for (JsonNode document : List.of(generated, documented)) {
				JsonNode schema = document.at("/components/schemas/RefundRequestResponse");
				for (String field : List.of("memberCode", "memberFullName", "orderNumber", "paymentReference", "receiptNumber")) {
					JsonNode property = schema.path("properties").path(field);
					assertThat(property.isMissingNode()).as(field).isFalse();
					assertThat(property.path("type").toString()).contains("string");
					assertThat(property.path("nullable").asBoolean() || property.path("type").toString().contains("null"))
						.as(field + " is nullable").isTrue();
				}
				for (String field : List.of("id", "paymentId", "orderId", "membershipId", "receiptId", "memberAccountId")) {
					assertThat(schema.path("properties").path(field).path("format").asText()).as(field).isEqualTo("uuid");
				}
				assertThat(schema.at("/properties/version/type").asText()).isEqualTo("integer");
				for (String role : List.of("reception", "manager")) {
					assertThat(response(document, "/" + role + "/refund-requests", "get", "200")
						.path("content").elements().next().at("/schema/$ref").asText())
						.isEqualTo("#/components/schemas/RefundRequestPageResponse");
				}
				assertThat(document.at("/components/schemas/RefundRequestPageResponse/properties/content/items/$ref").asText())
					.isEqualTo("#/components/schemas/RefundRequestResponse");
				for (String path : List.of("/reception/members/{memberCode}/refund-requests",
					"/reception/refund-requests/{requestId}/cash-disbursement",
					"/manager/refund-requests/{requestId}/first-approval",
					"/manager/refund-requests/{requestId}/final-approval", "/manager/refund-requests/{requestId}/reject")) {
					assertThat(response(document, path, "post", path.contains("{memberCode}") ? "201" : "200")
						.path("content").elements().next().at("/schema/$ref").asText())
						.isEqualTo("#/components/schemas/RefundRequestResponse");
				}
			}
		}
	}

	@Test
	void generatedContractDocumentsEmailVerificationAndManualActivation() throws Exception {
		JsonNode document = generatedDocument();
		assertThat(document.at("/components/schemas/RegistrationRequest/properties/fullName/pattern").asText())
			.isEqualTo(PersonNameRules.PATTERN);
		assertThat(document.at("/components/schemas/RegistrationResponse/properties").properties().stream()
			.map(java.util.Map.Entry::getKey).toList()).contains("verificationRequired", "maskedEmail", "csrfToken",
				"otpExpiresAt", "resendAvailableAt", "registrationExpiresAt", "serverTime");
		assertThat(document.at("/components/schemas/RegistrationResponse/properties/accountId").isMissingNode()).isTrue();
		assertThat(document.at("/components/schemas/VerifyEmailRequest/properties/otp/pattern").asText())
			.isEqualTo("^[0-9]{6}$");
		assertThat(response(document, "/auth/email-verification/resend", "post", "202").isMissingNode()).isFalse();
		for (String path : List.of("/auth/register", "/auth/email-verification/verify",
				"/auth/email-verification/resend", "/auth/email-verification/change-email")) {
			assertThat(response(document, path, "post", "429").isMissingNode()).as(path).isFalse();
		}
		assertThat(document.at("/components/schemas/ManualActivationPageResponse/properties/content/type").asText())
			.isEqualTo("array");
		JsonNode manualActivation = document.at("/components/schemas/ManualActivationResponse/properties");
		assertThat(manualActivation.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("reasonCode", "reasonDetail", "verificationMethod", "evidenceNote",
				"requestedByName", "requestedByRole", "reviewedByAccountId", "decisionNote");
		for (String path : List.of("/reception/email-verification/manual-activation-requests",
				"/manager/email-verification/manual-activation-requests")) {
			JsonNode parameters = operation(document, path, "get").path("parameters");
			assertThat(parameters.toString()).contains("status", "page", "size", "PENDING", "maximum");
		}
		assertThat(document.at("/paths/~1reception~1email-verification~1pending-accounts/get/description").asText())
			.contains("RECEPTIONIST");
		assertThat(document.at("/paths/~1manager~1email-verification~1manual-activation-requests/get/description").asText())
			.contains("MANAGER");
		assertThat(document.at("/components/schemas/StaffEmailChangeRequest/properties").properties().stream()
			.map(java.util.Map.Entry::getKey).toList()).containsExactlyInAnyOrder("newEmail", "currentPassword");
		assertThat(document.at("/components/schemas/StaffEmailChangeVerifyRequest/properties/otp/pattern").asText())
			.isEqualTo("^[0-9]{6}$");
		assertThat(document.at("/components/schemas/StaffProfileResponse/properties").properties().stream()
			.map(java.util.Map.Entry::getKey).toList()).contains("pendingEmailChange", "pendingEmailMasked",
				"pendingEmailOtpExpiresAt", "pendingEmailResendAvailableAt");
		assertThat(document.at("/components/schemas/StaffProfilePatchRequest/properties/email").isMissingNode()).isTrue();
		assertThat(document.at("/components/schemas/CreateTrainingPlanRequest/properties").properties().stream()
			.map(java.util.Map.Entry::getKey).toList()).contains("replaceActivePlanId", "expectedActivePlanVersion");
	}

	@Test
	void generatedContractDocumentsAuditActorIdentity() throws Exception {
		JsonNode properties = generatedDocument().at("/components/schemas/AuditHistoryResponse/properties");

		assertThat(properties.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("actorName", "actorRole");
		assertThat(properties.path("actorName").path("type").toString()).contains("string", "null");
		assertThat(properties.path("actorRole").path("type").toString()).contains("string", "null");
	}

	@Test
	void generatedContractDocumentsMembershipPlanManagement() throws Exception {
		JsonNode document = generatedDocument();
		JsonNode offerPlanCode = document.at("/components/schemas/MembershipOfferResponse/properties/planCode");
		assertThat(offerPlanCode.path("type").asText()).isEqualTo("string");
		assertThat(offerPlanCode.path("enum").isMissingNode()).isTrue();
		assertThat(offerPlanCode.path("pattern").asText()).isEqualTo("^[A-Z][A-Z0-9_]{1,49}$");
		JsonNode offerResponse = document.at("/components/schemas/MembershipOfferResponse/properties");
		assertThat(offerResponse.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("benefitCodes", "benefits", "supportsBooking", "supportsPersonalCoaching");
		JsonNode adminOfferResponse = document.at("/components/schemas/MembershipOfferAdminDetailResponse/properties");
		assertThat(adminOfferResponse.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("planDisplayName", "benefitCodes", "benefits");
		JsonNode orderPlanCode = document.at("/components/schemas/MembershipOrderResponse/properties/planCode");
		assertThat(orderPlanCode.path("type").asText()).isEqualTo("string");
		assertThat(orderPlanCode.path("enum").isMissingNode()).isTrue();
		assertThat(orderPlanCode.path("pattern").asText()).isEqualTo("^[A-Z][A-Z0-9_]{1,49}$");
		assertThat(document.at("/components/schemas/MembershipOrderResponse/properties/fulfillmentMode/enum").toString())
			.isEqualTo("[\"IMMEDIATE\",\"RESERVED\"]");
		assertThat(document.at("/components/schemas/MembershipOrderResponse/required").toString())
			.contains("fulfillmentMode");
		assertThat(document.at("/components/schemas/PaymentResultResponse/properties/fulfillmentMode/enum").toString())
			.isEqualTo("[\"IMMEDIATE\",\"RESERVED\"]");
		assertThat(document.at("/components/schemas/PaymentResultResponse/required").toString())
			.contains("fulfillmentMode");
		assertThat(document.at("/components/schemas/PaymentResultResponse/properties").properties()
			.stream().map(java.util.Map.Entry::getKey).toList())
			.contains("orderNumber", "memberCode", "memberName", "paymentReference");
		assertThat(document.at("/components/schemas/PaymentResultResponse/properties/paymentReference/type").toString())
			.isEqualTo("[\"string\",\"null\"]");
		assertThat(document.at("/components/schemas/ReceiptResponse/properties").properties()
			.stream().map(java.util.Map.Entry::getKey).toList())
			.contains("orderNumber", "paymentReference");
		assertThat(document.at("/components/schemas/ReceiptResponse/properties/paymentReference/type").toString())
			.isEqualTo("[\"string\",\"null\"]");
		assertThat(document.at("/components/schemas/ReconciliationQueueItem/properties").properties()
			.stream().map(java.util.Map.Entry::getKey).toList())
			.contains("memberCode", "paymentReference");
		assertThat(document.at("/components/schemas/ReconciliationQueueItem/properties/paymentReference/type").toString())
			.isEqualTo("[\"string\",\"null\"]");
		JsonNode response = document.at("/components/schemas/MembershipPlanResponse/properties");

		assertThat(response.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("code", "displayName", "status", "benefitCodes", "benefits", "createdAt",
				"updatedAt", "version", "benefitsLocked");
		assertThat(response.path("benefitsLocked").path("type").asText()).isEqualTo("boolean");
		assertThat(document.at("/components/schemas/MembershipPlanResponse/required").toString())
			.contains("benefitsLocked");
		assertThat(response.path("status").path("enum").toString())
			.isEqualTo("[\"ACTIVE\",\"INACTIVE\"]");
		assertThat(response.path("benefitCodes").path("type").asText()).isEqualTo("array");
		assertThat(response.path("createdAt").path("format").asText()).isEqualTo("date-time");
		assertThat(response.path("updatedAt").path("format").asText()).isEqualTo("date-time");

		JsonNode benefit = document.at("/components/schemas/MembershipBenefitResponse/properties");
		assertThat(benefit.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("code", "displayName", "description", "status", "displayOrder",
				"groupCode", "groupDisplayName", "groupDisplayOrder");
		assertThat(document.at("/components/schemas/MembershipBenefitResponse/required").toString())
			.contains("displayOrder", "groupCode", "groupDisplayName", "groupDisplayOrder");
		assertThat(benefit.at("/displayOrder/minimum").asInt()).isZero();
		assertThat(benefit.at("/groupCode/type").toString()).contains("string", "null");
		assertThat(benefit.at("/groupDisplayName/type").toString()).contains("string", "null");
		assertThat(benefit.at("/groupDisplayOrder/type").toString()).contains("integer", "null");
		assertThat(benefit.at("/groupDisplayOrder/minimum").asInt()).isZero();
		assertThat(document.at("/components/schemas/MembershipPlanResponse/required").toString())
			.contains("benefits");

		assertThat(operation(document, "/manager/membership-benefits", "get").path("description").asText())
			.isEqualTo("Requires MANAGER role.");
		assertThat(response(document, "/manager/membership-plans/{planCode}", "patch", "409")
			.path("$ref").asText()).isEqualTo("#/components/responses/Conflict");
		assertThat(operation(document, "/manager/membership-plans", "post")
			.at("/requestBody/required").asBoolean()).isTrue();
		assertThat(operation(document, "/manager/membership-plans/{planCode}", "patch")
			.at("/requestBody/required").asBoolean()).isTrue();
		assertThat(document.path("paths").path("/manager/membership-plans/{planCode}").has("delete"))
			.isFalse();
	}

	@Test
	void generatedMembershipRequestContractsIncludeManagerMemberIdentity() throws Exception {
		JsonNode document = generatedDocument();
		for (String schemaName : List.of("MembershipTerminationRequestResponse", "MembershipFreezeRequestResponse")) {
			JsonNode properties = document.at("/components/schemas/" + schemaName + "/properties");
			assertThat(properties.properties().stream().map(java.util.Map.Entry::getKey).toList())
				.contains("memberId", "memberFullName");
		}
	}

	@Test
	void staticContractDocumentsAuditActorIdentity() throws Exception {
		var resource = getClass().getResource("/openapi/us52-manager-audit-history.yaml");
		assertThat(resource).isNotNull();
		JsonNode document = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
			.readTree(resource);
		JsonNode properties = document.at("/components/schemas/AuditHistoryResponse/properties");

		assertThat(properties.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("actorAccountId", "actorName", "actorRole");
		assertThat(properties.path("actorName").path("type").toString()).contains("string", "null");
		assertThat(properties.path("actorRole").path("type").toString()).contains("string", "null");
	}

	@Test
	void generatedContractDocumentsCenterVisitMemberName() throws Exception {
		JsonNode schema = generatedDocument().at("/components/schemas/CenterVisitResponse");

		assertThat(schema.path("properties").properties().stream()
			.map(java.util.Map.Entry::getKey).toList()).contains("memberName");
		assertThat(schema.path("required").toString()).contains("memberName");
	}

	@Test
	void staticContractDocumentsCenterVisitMemberName() throws Exception {
		var resource = getClass().getResource("/openapi/us36-center-visit.yaml");
		assertThat(resource).isNotNull();
		JsonNode document = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
			.readTree(resource);
		JsonNode schema = document.at("/components/schemas/CenterVisitResponse");

		assertThat(schema.path("properties").properties().stream()
			.map(java.util.Map.Entry::getKey).toList()).contains("memberName");
		assertThat(schema.path("required").toString()).contains("memberName");
	}

	@Test
	void staticOfferContractsDocumentPublicActiveCatalogue() throws Exception {
		ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
		for (String resourcePath : List.of("/openapi/us12-membership-offer-list.yaml",
			"/openapi/us13-membership-offer-detail.yaml")) {
			try (var stream = getClass().getResourceAsStream(resourcePath)) {
				assertThat(stream).isNotNull();
				JsonNode document = yaml.readTree(stream);
				JsonNode operation = document.at(resourcePath.contains("us12")
					? "/paths/~1membership-offers/get"
					: "/paths/~1membership-offers~1{offerId}/get");
				assertThat(operation.path("security").isMissingNode()).as(resourcePath).isTrue();
				assertThat(operation.path("responses").path("401").isMissingNode()).as(resourcePath).isTrue();
				assertThat(operation.path("responses").path("403").isMissingNode()).as(resourcePath).isTrue();
				assertThat(operation.path("description").asText().toLowerCase()).as(resourcePath)
					.contains("public", "active", "plan");
			}
		}
	}

	@Test
	void generatedContractDocumentsAssignmentDiscovery() throws Exception {
		JsonNode document = generatedDocument();
		JsonNode candidates = operation(document, "/manager/coach-assignments/candidates", "get");
		assertThat(candidates.path("parameters").toString())
			.contains("query", "assignmentStatus", "page", "size", "UNASSIGNED", "ASSIGNED", "ALL");
		assertThat(response(document, "/manager/coach-assignments/candidates", "get", "200")
			.at("/content/application~1json/schema/$ref").asText())
			.isEqualTo("#/components/schemas/AssignmentCandidatePageResponse");

		JsonNode workloads = operation(document, "/manager/coaches/assignment-workloads", "get");
		assertThat(response(document, "/manager/coaches/assignment-workloads", "get", "200")
			.at("/content/application~1json/schema/type").asText()).isEqualTo("array");
		assertThat(response(document, "/manager/coaches/assignment-workloads", "get", "200")
			.at("/content/application~1json/schema/items/$ref").asText())
			.isEqualTo("#/components/schemas/CoachAssignmentWorkloadResponse");

		JsonNode workload = document.at("/components/schemas/CoachAssignmentWorkloadResponse/properties");
		assertThat(workload.path("activeMemberCount").path("format").asText()).isEqualTo("int64");
		assertThat(workload.path("maximumMemberCount").path("type").asText()).isEqualTo("integer");
		assertThat(workload.path("availableForAssignment").path("type").asText()).isEqualTo("boolean");
	}

	@Test
	void generatedContractDocumentsTrainingPlanGapContracts() throws Exception {
		JsonNode document = generatedDocument();
		JsonNode response = document.at("/components/schemas/TrainingPlanResponse/properties");
		assertThat(response.path("updatedAt").path("format").asText()).isEqualTo("date-time");
		assertThat(response.path("versionCreatedAt").path("format").asText()).isEqualTo("date-time");
		JsonNode update = document.at("/components/schemas/UpdateTrainingPlanRequest");
		assertThat(update.path("required").toString()).contains("expectedVersionNumber");
		assertThat(update.at("/properties/expectedVersionNumber/minimum").asInt()).isEqualTo(1);
		assertThat(document.at("/components/schemas/TrainingPlanContent/properties").properties()
				.stream().map(java.util.Map.Entry::getKey).toList())
				.containsExactlyInAnyOrder("goal", "schedule", "exercises", "notes");
		assertThat(response.path("contentSchemaVersion").path("type").asText()).isEqualTo("integer");
		assertThat(response(document, "/coach/members/{memberId}/training-plans/{planId}/versions", "get", "200")
				.isMissingNode()).isFalse();
		assertThat(response(document, "/coach/members/{memberId}/training-plans/{planId}/archive", "post", "200")
				.isMissingNode()).isFalse();
	}

	@Test
	void generatedContractDocumentsTrainingBenefitEligibility() throws Exception {
		JsonNode document = generatedDocument();
		for (var requirement : java.util.Map.ofEntries(
			java.util.Map.entry("POST /manager/members/{memberId}/coach-assignment", "PERSONAL_COACHING"),
			java.util.Map.entry("GET /manager/coach-assignments/candidates", "PERSONAL_COACHING"),
			java.util.Map.entry("POST /coach/members/{memberId}/training-plans", "TRAINING_PLAN"),
			java.util.Map.entry("PUT /coach/members/{memberId}/training-plans/{planId}", "TRAINING_PLAN"),
			java.util.Map.entry("POST /coach/members/{memberId}/training-results", "TRAINING_RESULT"),
			java.util.Map.entry("PUT /coach/members/{memberId}/training-results/{resultId}", "TRAINING_RESULT"),
			java.util.Map.entry("POST /coach/members/{memberId}/feedback", "TRAINING_FEEDBACK"),
			java.util.Map.entry("PUT /coach/members/{memberId}/feedback/{feedbackId}", "TRAINING_FEEDBACK"),
			java.util.Map.entry("POST /coach/class-sessions/{sessionId}/members/{memberId}/training-results",
				"TRAINING_RESULT"),
			java.util.Map.entry("PUT /coach/class-sessions/{sessionId}/members/{memberId}/training-results/{resultId}",
				"TRAINING_RESULT"),
			java.util.Map.entry("POST /coach/class-sessions/{sessionId}/members/{memberId}/feedback",
				"TRAINING_FEEDBACK"),
			java.util.Map.entry("PUT /coach/class-sessions/{sessionId}/members/{memberId}/feedback/{feedbackId}",
				"TRAINING_FEEDBACK")).entrySet()) {
			int separator = requirement.getKey().indexOf(' ');
			String method = requirement.getKey().substring(0, separator).toLowerCase();
			String path = requirement.getKey().substring(separator + 1);
			assertThat(operation(document, path, method).path("description").asText())
				.as(requirement.getKey()).contains(requirement.getValue(), "Benefit");
		}
	}

	@Test
	void recurringScheduleContractDocumentsOptionalBoundedSessionCount() throws Exception {
		JsonNode generated = generatedDocument();
		JsonNode generatedCount = generated.at(
			"/components/schemas/RecurringScheduleCreateRequest/properties/sessionCount");
		assertThat(generatedCount.path("minimum").asInt()).isEqualTo(1);
		assertThat(generatedCount.path("maximum").asInt()).isEqualTo(100);
		assertThat(generatedCount.path("default").asInt()).isEqualTo(30);
		assertThat(generated.at("/components/schemas/RecurringScheduleCreateRequest/required").toString())
			.doesNotContain("sessionCount");

		try (var stream = getClass().getResourceAsStream("/openapi/us23-manager-scheduling-changes.yaml")) {
			JsonNode documented = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
				.readTree(stream);
			JsonNode documentedCount = documented.at(
				"/components/schemas/RecurringScheduleCreateRequest/properties/sessionCount");
			assertThat(documentedCount.path("minimum").asInt()).isEqualTo(1);
			assertThat(documentedCount.path("maximum").asInt()).isEqualTo(100);
			assertThat(documentedCount.path("default").asInt()).isEqualTo(30);
			assertThat(response(documented, "/manager/recurring-schedules", "post", "201")
				.isMissingNode()).isFalse();
		}
	}

	@Test
	void generatedAndStaticContractsDescribeRoomSuspensionExtension() throws Exception {
		JsonNode generated = generatedDocument();
		JsonNode generatedPatch = operation(generated,
			"/manager/rooms/{roomId}/suspensions/{suspensionId}", "patch");
		assertThat(generatedPatch.path("requestBody").path("content").path("application/json")
			.path("schema").path("$ref").asText())
			.isEqualTo("#/components/schemas/RoomSuspensionExtensionRequest");
		JsonNode generatedRequest = generated.at("/components/schemas/RoomSuspensionExtensionRequest/properties");
		assertThat(generatedRequest.path("expectedEndTime").path("format").asText()).isEqualTo("date-time");
		assertThat(generatedRequest.path("endTime").path("format").asText()).isEqualTo("date-time");
		assertThat(generatedRequest.path("resolutions").path("type").asText()).isEqualTo("array");
		assertThat(generatedRequest.path("bulkResolution").path("$ref").asText())
			.isEqualTo("#/components/schemas/RoomBulkResolutionRequest");
		assertThat(generatedRequest.path("resolutionOverrides").path("type").asText()).isEqualTo("array");
		assertThat(generated.at("/components/schemas/RoomSuspensionExtensionRequest/required").toString())
			.contains("expectedEndTime", "endTime", "reason")
			.doesNotContain("resolutions", "bulkResolution");
		assertThat(generated.at("/components/schemas/RoomBulkResolutionRequest/properties/action/enum").toString())
			.contains("MOVE", "CANCEL").doesNotContain("RESCHEDULE");
		assertThat(generated.at("/components/schemas/RoomBulkResolutionRequest/required").toString())
			.contains("action");
		assertThat(response(generated, "/manager/rooms/{roomId}/suspensions/{suspensionId}", "patch", "409")
			.isMissingNode()).isFalse();

		try (var stream = getClass().getResourceAsStream("/openapi/us23-manager-scheduling-changes.yaml")) {
			JsonNode documented = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
				.readTree(stream);
			JsonNode documentedPatch = operation(documented,
				"/manager/rooms/{roomId}/suspensions/{suspensionId}", "patch");
			assertThat(documentedPatch.path("requestBody").path("content").path("application/json")
				.path("schema").path("$ref").asText())
				.isEqualTo("#/components/schemas/RoomSuspensionExtensionRequest");
			assertThat(documented.at("/components/schemas/RoomSuspensionExtensionRequest/required").toString())
				.contains("expectedEndTime", "endTime", "reason").doesNotContain("resolutions");
			assertThat(documented.at("/components/schemas/RoomImpactPreviewRequest/properties/bulkResolution/$ref")
				.asText()).isEqualTo("#/components/schemas/RoomBulkResolutionRequest");
			assertThat(documented.at("/components/schemas/RoomBulkResolutionRequest/properties/action/enum")
				.toString()).contains("MOVE", "CANCEL").doesNotContain("RESCHEDULE");
			assertThat(documented.at("/components/schemas/RoomBulkResolutionRequest/required").toString())
				.contains("action");
			assertThat(response(documented, "/manager/rooms/{roomId}/suspensions/{suspensionId}", "patch", "409")
				.path("$ref").asText()).isEqualTo("#/components/responses/Conflict");
		}
	}

	@Test
	void receptionSearchDocumentsNullableActiveMembershipInGeneratedAndStaticContracts() throws Exception {
		JsonNode generated = generatedDocument();
		assertReceptionMembershipSummary(generated.at("/components/schemas/ReceptionMemberSearchResponse/properties/activeMembership"));
		try (var input = getClass().getResourceAsStream("/openapi/us11-receptionist-member-profile.yaml")) {
			JsonNode contract = new ObjectMapper(new YAMLFactory()).readTree(input);
			assertReceptionMembershipSummary(contract.at("/components/schemas/ReceptionActiveMembershipResponse"));
		}
	}

	@Test
	void staffProfileContractsKeepImageUploadOutOfPatchAndRequireNullableImageInResponses() throws Exception {
		assertStaffProfileSchemas(generatedDocument());
		try (var input = getClass().getResourceAsStream("/openapi/staff-self-profile.yaml")) {
			assertThat(input).isNotNull();
			assertStaffProfileSchemas(new ObjectMapper(new YAMLFactory()).readTree(input));
		}
	}

	@Test
	void generatedCoachLeaveResponseIncludesCoachIdentity() throws Exception {
		JsonNode response = generatedDocument().at("/components/schemas/CoachLeaveResponse");
		assertThat(response.path("properties").has("coachId")).isTrue();
		assertThat(response.path("properties").has("coachFullName")).isTrue();
		assertThat(response.path("properties").has("coachPhone")).isTrue();
	}

	@Test
	void receptionSearchRequiresExplicitNullableMembershipInGeneratedAndStaticContracts() throws Exception {
		JsonNode generated = generatedDocument().at("/components/schemas/ReceptionMemberSearchResponse");
		JsonNode membership = generated.at("/properties/activeMembership");
		assertThat(membership.path("type")).as(membership.toString()).containsExactlyInAnyOrder(
			objectMapper.getNodeFactory().textNode("object"), objectMapper.getNodeFactory().textNode("null"));
		assertThat(membership.has("$ref")).as("An object-only reference would reject null despite the nullable type union").isFalse();
		assertThat(membership.has("allOf") || membership.has("oneOf") || membership.has("anyOf")).isFalse();
		assertReceptionMembershipSummary(membership);
		assertThat(generated.path("required")).contains(objectMapper.getNodeFactory().textNode("activeMembership"));
		try (var input = getClass().getResourceAsStream("/openapi/us11-receptionist-member-profile.yaml")) {
			JsonNode contract = new ObjectMapper(new YAMLFactory()).readTree(input)
				.at("/components/schemas/MemberSearchResponse");
			assertThat(contract.path("required")).contains(objectMapper.getNodeFactory().textNode("activeMembership"));
			assertThat(contract.at("/properties/activeMembership/oneOf")).isEqualTo(objectMapper.readTree("""
				[{"$ref":"#/components/schemas/ReceptionActiveMembershipResponse"},{"type":"null"}]
				"""));
		}
	}

	private void assertReceptionMembershipSummary(JsonNode schema) {
		assertThat(schema.path("properties").properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("membershipId", "offerName", "planName", "effectiveStartDate",
				"effectiveEndDate", "status", "benefitCodes", "frozen");
		assertThat(schema.at("/properties/membershipId/format").asText()).isEqualTo("uuid");
		assertThat(schema.at("/properties/effectiveStartDate/format").asText()).isEqualTo("date");
		assertThat(schema.at("/properties/effectiveEndDate/format").asText()).isEqualTo("date");
		assertThat(schema.at("/properties/benefitCodes/uniqueItems").asBoolean()).isTrue();
		assertThat(schema.at("/properties/frozen/type").asText()).isEqualTo("boolean");
		assertThat(schema.at("/properties/status/enum")).containsExactly(objectMapper.getNodeFactory().textNode("ACTIVE"));
	}

	private void assertStaffProfileSchemas(JsonNode document) {
		JsonNode patch = document.at("/components/schemas/StaffProfilePatchRequest");
		assertThat(patch.path("additionalProperties").asBoolean()).isFalse();
		assertThat(patch.path("properties").has("profileImageUrl")).isFalse();
		JsonNode response = document.at("/components/schemas/StaffProfileResponse");
		assertThat(response.path("required"))
			.contains(objectMapper.getNodeFactory().textNode("profileImageUrl"));
		assertThat(response.at("/properties/profileImageUrl/type")).containsExactlyInAnyOrder(
			objectMapper.getNodeFactory().textNode("string"), objectMapper.getNodeFactory().textNode("null"));
	}

	private JsonNode generatedDocument() throws Exception {
		return objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
	}

	private Set<String> operations(JsonNode document) {
		Set<String> operations = new HashSet<>();
		document.path("paths").properties().forEach(pathEntry ->
			pathEntry.getValue().properties().forEach(operationEntry -> {
				if (HTTP_METHODS.contains(operationEntry.getKey())) {
					operations.add(operationEntry.getKey().toUpperCase() + " " + pathEntry.getKey());
				}
			}));
		return operations;
	}

	private void assertProblemReference(JsonNode document, String path, String method, String status) {
		assertThat(response(document, path, method, status).path("$ref").asText())
			.startsWith("#/components/responses/");
	}

	private JsonNode operation(JsonNode document, String path, String method) {
		return document.path("paths").path(path).path(method);
	}

	private JsonNode response(JsonNode document, String path, String method, String status) {
		return operation(document, path, method).path("responses").path(status);
	}

	private Set<String> responseStatuses(JsonNode document, CourseOperationContract contract) {
		return operation(document, contract.path(), contract.method()).path("responses").properties().stream()
			.map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
	}

	private void assertExpectedCatalogContract(JsonNode document, String source) {
		for (CatalogParameterContract contract : COURSE_CATALOG_PARAMETER_CONTRACTS) {
			JsonNode parameter = parameter(document, contract.path(), contract.name());
			String label = source + " GET " + contract.path() + " parameter " + contract.name();
			assertThat(parameter.path("in").asText()).as(label + " location").isEqualTo(contract.location());
			assertThat(parameter.path("required").asBoolean(false)).as(label + " required")
				.isEqualTo(contract.required());
			assertSchemaMetadata(document, parameter.path("schema"), contract.types(), contract.format(),
				contract.defaultValue(), contract.minimum(), contract.maximum(), contract.enumValues(), null, label);
		}

		for (CatalogSchemaContract contract : COURSE_CATALOG_SCHEMA_CONTRACTS) {
			JsonNode schema = document.at("/components/schemas/" + contract.name());
			String label = source + " schema " + contract.name();
			Set<String> expectedProperties = contract.properties().stream()
				.map(CatalogPropertyContract::name).collect(java.util.stream.Collectors.toSet());
			assertThat(schema.path("properties").properties().stream().map(java.util.Map.Entry::getKey).toList())
				.as(label + " properties").containsExactlyInAnyOrderElementsOf(expectedProperties);
			assertThat(schema.path("required").values()).as(label + " required").toIterable()
				.extracting(JsonNode::asText).containsExactlyInAnyOrderElementsOf(expectedProperties);
			for (CatalogPropertyContract property : contract.properties()) {
				assertSchemaMetadata(document, schema.path("properties").path(property.name()), property.types(),
					property.format(), null, property.minimum(), property.maximum(), property.enumValues(),
					property.itemSchema(), label + " property " + property.name());
			}
		}
	}

	private JsonNode parameter(JsonNode document, String path, String name) {
		return java.util.stream.StreamSupport.stream(operation(document, path, "get").path("parameters").spliterator(),
			false).map(parameter -> resolve(document, parameter))
			.filter(parameter -> name.equals(parameter.path("name").asText())).findFirst().orElseThrow();
	}

	private void assertSchemaMetadata(JsonNode document, JsonNode rawSchema, Set<String> expectedTypes,
			String expectedFormat, String expectedDefault, String expectedMinimum, String expectedMaximum,
			Set<String> expectedEnum, String expectedItemSchema, String label) {
		JsonNode schema = resolve(document, rawSchema);
		Set<String> actualTypes = schema.path("type").isArray()
			? java.util.stream.StreamSupport.stream(schema.path("type").spliterator(), false)
				.map(JsonNode::asText).collect(java.util.stream.Collectors.toSet())
			: Set.of(schema.path("type").asText());
		assertThat(actualTypes).as(label + " type").containsExactlyInAnyOrderElementsOf(expectedTypes);
		assertThat(textOrNull(schema, "format")).as(label + " format").isEqualTo(expectedFormat);
		assertThat(textOrNull(schema, "default")).as(label + " default").isEqualTo(expectedDefault);
		assertThat(textOrNull(schema, "minimum")).as(label + " minimum").isEqualTo(expectedMinimum);
		assertThat(textOrNull(schema, "maximum")).as(label + " maximum").isEqualTo(expectedMaximum);
		assertThat(schema.path("enum").values()).as(label + " enum").toIterable().extracting(JsonNode::asText)
			.containsExactlyInAnyOrderElementsOf(expectedEnum);
		if (expectedItemSchema == null) {
			assertThat(schema.has("items")).as(label + " items").isFalse();
		}
		else {
			assertThat(schema.at("/items/$ref").asText()).as(label + " items")
				.isEqualTo("#/components/schemas/" + expectedItemSchema);
		}
	}

	private String textOrNull(JsonNode node, String field) {
		return node.has(field) ? node.path(field).asText() : null;
	}

	private ObjectNode normalizedCatalogContract(JsonNode document) {
		ObjectNode normalized = objectMapper.createObjectNode();
		for (CourseOperationContract contract : COURSE_CATALOG_OPERATION_CONTRACTS) {
			ObjectNode operation = objectMapper.createObjectNode();
			JsonNode source = operation(document, contract.path(), contract.method());
			operation.set("security", source.path("security").deepCopy());
			operation.set("parameters", normalizedParameters(document, source.path("parameters")));
			ObjectNode responses = objectMapper.createObjectNode();
			new TreeSet<>(responseStatuses(document, contract)).forEach(status ->
				responses.set(status, normalizedResponse(document,
					response(document, contract.path(), contract.method(), status))));
			operation.set("responses", responses);
			normalized.set(contract.method().toUpperCase() + " " + contract.path(), operation);
		}
		return normalized;
	}

	private ArrayNode normalizedParameters(JsonNode document, JsonNode parameters) {
		ArrayNode normalized = objectMapper.createArrayNode();
		List<ObjectNode> values = new ArrayList<>();
		parameters.forEach(parameter -> {
			JsonNode source = resolve(document, parameter);
			ObjectNode value = objectMapper.createObjectNode();
			value.put("name", source.path("name").asText());
			value.put("in", source.path("in").asText());
			value.put("required", source.path("required").asBoolean(false));
			value.set("schema", normalizedSchema(document, source.path("schema")));
			values.add(value);
		});
		values.stream().sorted(Comparator.comparing(value ->
			value.path("in").asText() + ":" + value.path("name").asText())).forEach(normalized::add);
		return normalized;
	}

	private ObjectNode normalizedResponse(JsonNode document, JsonNode response) {
		JsonNode source = resolve(document, response);
		ObjectNode normalized = objectMapper.createObjectNode();
		ObjectNode content = objectMapper.createObjectNode();
		source.path("content").properties().stream().sorted(java.util.Map.Entry.comparingByKey())
			.forEach(entry -> content.set(entry.getKey(),
				normalizedSchema(document, entry.getValue().path("schema"))));
		normalized.set("content", content);
		return normalized;
	}

	private ObjectNode normalizedSchema(JsonNode document, JsonNode schema) {
		JsonNode source = resolve(document, schema);
		ObjectNode normalized = objectMapper.createObjectNode();
		copy(source, normalized, "type", "format", "default", "minimum", "maximum", "exclusiveMinimum",
			"exclusiveMaximum", "minLength", "maxLength", "minItems", "maxItems", "minProperties",
			"maxProperties", "pattern", "multipleOf", "nullable");
		copySortedArray(source, normalized, "required");
		copySortedArray(source, normalized, "enum");
		if (source.has("properties")) {
			ObjectNode properties = objectMapper.createObjectNode();
			source.path("properties").properties().stream().sorted(java.util.Map.Entry.comparingByKey())
				.forEach(entry -> properties.set(entry.getKey(), normalizedSchema(document, entry.getValue())));
			normalized.set("properties", properties);
		}
		if (source.has("items")) normalized.set("items", normalizedSchema(document, source.path("items")));
		if (source.has("additionalProperties")) {
			JsonNode additional = source.path("additionalProperties");
			normalized.set("additionalProperties", additional.isObject()
				? normalizedSchema(document, additional) : additional.deepCopy());
		}
		for (String composition : List.of("allOf", "oneOf", "anyOf")) {
			if (source.has(composition)) {
				ArrayNode alternatives = objectMapper.createArrayNode();
				source.path(composition).forEach(item -> alternatives.add(normalizedSchema(document, item)));
				normalized.set(composition, alternatives);
			}
		}
		return normalized;
	}

	private JsonNode resolve(JsonNode document, JsonNode node) {
		JsonNode resolved = node;
		Set<String> visited = new HashSet<>();
		while (resolved.has("$ref")) {
			String reference = resolved.path("$ref").asText();
			if (!reference.startsWith("#/") || !visited.add(reference)) {
				throw new IllegalArgumentException("Unsupported OpenAPI reference " + reference);
			}
			resolved = document.at(reference.substring(1));
		}
		return resolved;
	}

	private void copy(JsonNode source, ObjectNode target, String... fields) {
		for (String field : fields) {
			if (source.has(field)) target.set(field, source.path(field).deepCopy());
		}
	}

	private void copySortedArray(JsonNode source, ObjectNode target, String field) {
		if (!source.has(field)) return;
		ArrayNode values = objectMapper.createArrayNode();
		java.util.stream.StreamSupport.stream(source.path(field).spliterator(), false)
			.sorted(Comparator.comparing(JsonNode::toString)).forEach(values::add);
		target.set(field, values);
	}

	private void assertProblemResponse(JsonNode document, CourseOperationContract contract, String status,
			String label, String expectedSchema) {
		String responseRef = response(document, contract.path(), contract.method(), status).path("$ref").asText();
		assertThat(responseRef).as(label + " " + status).startsWith("#/components/responses/");
		String component = responseRef.substring(responseRef.lastIndexOf('/') + 1);
		assertThat(document.at("/components/responses/" + component
			+ "/content/application~1problem+json/schema/$ref").asText())
			.as(label + " " + status).isEqualTo("#/components/schemas/" + expectedSchema);
	}

	private void assertConcreteSchema(JsonNode response, String label) {
		JsonNode content = response.path("content");
		boolean hasSchema = content.isObject() && content.properties().stream()
			.map(java.util.Map.Entry::getValue)
			.map(mediaType -> mediaType.path("schema"))
			.anyMatch(schema -> !schema.isMissingNode() && !schema.isEmpty());
		assertThat(hasSchema).as(label).isTrue();
	}

	private void assertRefundEligibleSourceSchema(JsonNode schema) {
		assertThat(schema.path("properties").properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("paymentId", "membershipId", "receiptId", "receiptNumber", "orderNumber",
				"offerName", "memberCode", "memberFullName", "paymentReference", "type", "paidAmount",
				"suggestedAmount", "currency", "calculationDate");
		assertThat(schema.path("required").toString()).contains("paymentId", "membershipId", "receiptId",
			"receiptNumber", "orderNumber", "offerName", "memberCode", "memberFullName", "type", "paidAmount",
			"paymentReference", "suggestedAmount", "currency", "calculationDate");
		assertThat(schema.at("/properties/calculationDate/format").asText()).isEqualTo("date");
		assertThat(schema.at("/properties/type/enum").toString())
			.isEqualTo("[\"TERMINATED_MEMBERSHIP\",\"RESERVED_CANCELLATION\"]");
	}

	private static ClassPathScanningCandidateComponentProvider controllerScanner() {
		ClassPathScanningCandidateComponentProvider scanner =
			new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
		return scanner;
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ControllerDependencyMockConfiguration {

		@Bean
		static BeanFactoryPostProcessor controllerDependencyMocks() {
			return beanFactory -> controllerScanner().findCandidateComponents("com.scms.backend").stream()
				.map(candidate -> load(candidate.getBeanClassName()))
				.filter(controller -> !controller.getName().contains("$"))
				.map(controller -> Arrays.stream(controller.getDeclaredConstructors())
					.max(Comparator.comparingInt(constructor -> constructor.getParameterCount()))
					.orElseThrow())
				.flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
				.distinct()
				.filter(dependency -> beanFactory.getBeanNamesForType(dependency, false, false).length == 0)
				.forEach(dependency -> beanFactory.registerSingleton(dependency.getName(), mock(dependency)));
		}

		private static Class<?> load(String className) {
			try {
				return Class.forName(className);
			}
			catch (ClassNotFoundException exception) {
				throw new IllegalStateException("Cannot load controller " + className, exception);
			}
		}
	}

	private record CourseOperationContract(String path, String method, Set<String> statuses) { }

	private record CatalogParameterContract(String path, String name, String location, boolean required,
		Set<String> types, String format, String defaultValue, String minimum, String maximum,
		Set<String> enumValues) { }

	private record CatalogSchemaContract(String name, List<CatalogPropertyContract> properties) { }

	private record CatalogPropertyContract(String name, Set<String> types, String format, String minimum,
		String maximum, Set<String> enumValues, String itemSchema) { }
}
