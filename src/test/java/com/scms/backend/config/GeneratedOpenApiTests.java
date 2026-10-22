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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.jupiter.api.Test;
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
		"POST /members/me/membership-orders",
		"GET /members/me/membership-orders/pending",
		"POST /reception/members/{memberId}/membership-orders",
		"GET /reception/members/{memberId}/membership-orders/pending",
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
		"GET /manager/rooms",
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
		"PATCH /manager/class-sessions/{id}/assignment",
		"PATCH /manager/class-sessions/{id}/cancel",
		"POST /manager/class-sessions/cancellation-preview",
		"POST /manager/class-sessions/bulk-cancel",
		"POST /manager/recurring-schedules",
		"GET /members/class-sessions",
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
			.isEqualTo("[\"RESERVED\",\"ACTIVE\",\"EXPIRED\",\"TERMINATED\"]");
		assertThat(schema.at("/properties/benefitCodes/type").asText()).isEqualTo("array");
		assertThat(schema.at("/properties/benefits/items/$ref").asText())
			.isEqualTo("#/components/schemas/MembershipBenefitResponse");
		JsonNode memberHistory = document.at("/components/schemas/MembershipHistoryResponse/properties");
		assertThat(memberHistory.properties().stream().map(java.util.Map.Entry::getKey).toList())
			.contains("benefitCodes", "benefits");
	}

	@Test
	void generatedContractDocumentsEmailVerificationAndManualActivation() throws Exception {
		JsonNode document = generatedDocument();
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
				"reviewedByAccountId", "decisionNote");
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
		assertThat(generated.at("/components/schemas/RoomSuspensionExtensionRequest/required").toString())
			.contains("expectedEndTime", "endTime", "reason", "resolutions");
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
				.contains("expectedEndTime", "endTime", "reason", "resolutions");
			assertThat(response(documented, "/manager/rooms/{roomId}/suspensions/{suspensionId}", "patch", "409")
				.path("$ref").asText()).isEqualTo("#/components/responses/Conflict");
		}
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

	private void assertConcreteSchema(JsonNode response, String label) {
		JsonNode content = response.path("content");
		boolean hasSchema = content.isObject() && content.properties().stream()
			.map(java.util.Map.Entry::getValue)
			.map(mediaType -> mediaType.path("schema"))
			.anyMatch(schema -> !schema.isMissingNode() && !schema.isEmpty());
		assertThat(hasSchema).as(label).isTrue();
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
}
