package com.scms.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SCMS Backend API", version = "v1"),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfiguration {

	private static final String PROBLEM_SCHEMA = "#/components/schemas/ApiProblem";
	private static final String UNAUTHORIZED_PROBLEM_SCHEMA = "#/components/schemas/UnauthorizedProblem";
	private static final List<String> UNAUTHORIZED_CODES = List.of(
		"UNAUTHENTICATED", "INVALID_TOKEN", "INVALID_CREDENTIALS", "INVALID_REFRESH_TOKEN",
		"INVALID_PENDING_SESSION", "INVALID_PASSWORD_RESET_SESSION", "PAYMENT_UNAUTHORIZED");

	@Bean
	OpenApiCustomizer apiContractCustomizer() {
		return openApi -> {
			Components components = openApi.getComponents() == null ? new Components() : openApi.getComponents();
			openApi.setComponents(components);
			components.addSchemas("ApiProblem", problemSchema());
			components.addSchemas("UnauthorizedProblem", unauthorizedProblemSchema());
			components.addSchemas("OrdinaryBookingConflictProblem", ordinaryBookingConflictProblemSchema());
			components.addSchemas("MembershipBenefitRequiredProblem", membershipBenefitRequiredProblemSchema());
			Schema<?> coursePaymentProvider = (Schema<?>) components.getSchemas()
				.get("CoursePaymentResponse").getProperties().get("provider");
			((Schema) coursePaymentProvider).setEnum(java.util.Arrays.asList("SEPAY", null));
			addErrorResponse(components, "BadRequest", "The request is invalid.");
			addErrorResponse(components, "Unauthorized",
				"Authentication failed (UNAUTHENTICATED, INVALID_TOKEN, INVALID_CREDENTIALS, "
					+ "INVALID_REFRESH_TOKEN, INVALID_PENDING_SESSION, INVALID_PASSWORD_RESET_SESSION, "
					+ "or PAYMENT_UNAUTHORIZED).",
				UNAUTHORIZED_PROBLEM_SCHEMA);
			addErrorResponse(components, "Forbidden",
				"The authenticated account is not allowed to perform this operation (FORBIDDEN).");
			addErrorResponse(components, "NotFound", "The requested resource was not found.");
			addErrorResponse(components, "Conflict", "The request conflicts with current resource state.");
			addBookingConflictResponse(components);
			addErrorResponse(components, "MembershipOrderConflict",
				"Membership Order creation conflicts with current state; an unavailable Offer uses code MEMBERSHIP_OFFER_NOT_AVAILABLE.");
			addErrorResponse(components, "TooManyRequests", "The request rate limit was exceeded.");

			openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
				boolean publicOperation = operation.getSecurity() != null && operation.getSecurity().isEmpty();
				if (operation.getDescription() == null) {
					operation.setDescription(constraint(path, publicOperation));
				}
				addDefaultResponse(operation, "400", "BadRequest");
				addDefaultResponse(operation, "401", "Unauthorized");
				if (!publicOperation) {
					addDefaultResponse(operation, "403", "Forbidden");
				}
				if (path.contains("{")) {
					addDefaultResponse(operation, "404", "NotFound");
				}
				if (method != HttpMethod.GET) {
					addDefaultResponse(operation, "409", "Conflict");
				}
				if (path.contains("email-verification") || path.contains("password-reset") || path.contains("email-change")
						|| path.equals("/auth/register")) {
					addDefaultResponse(operation, "429", "TooManyRequests");
				}
				applyKnownResponseVariants(path, method, operation);
			}));
		};
	}

	private void addDefaultResponse(Operation operation, String status, String component) {
		if (!operation.getResponses().containsKey(status)) {
			operation.getResponses().addApiResponse(status, reference(component));
		}
	}

	private void applyKnownResponseVariants(String path, HttpMethod method, Operation operation) {
		if (method == HttpMethod.POST && (path.equals("/manager/courses")
				|| path.equals("/manager/course-runs")
				|| path.equals("/manager/course-runs/{runId}/sessions"))) {
			ApiResponse ok = operation.getResponses().get("200");
			operation.getResponses().remove("200");
			operation.getResponses().addApiResponse("201",
				new ApiResponse().description("Created").content(ok.getContent()));
		}
		if (method == HttpMethod.POST && path.equals("/manager/course-runs")) {
			operation.getResponses().addApiResponse("404", reference("NotFound"));
		}
		if (method == HttpMethod.GET && path.equals("/reception/members/search")) {
			operation.getResponses().addApiResponse("404", reference("NotFound"));
		}
		if (method == HttpMethod.GET && path.equals("/coach/class-sessions/{sessionId}/attendance")) {
			operation.getResponses().addApiResponse("409", reference("Conflict"));
		}
		if (method == HttpMethod.POST && path.equals("/reception/center-visits")) {
			ApiResponse ok = operation.getResponses().get("200");
			operation.getResponses().addApiResponse("201",
				new ApiResponse().description("Created").content(ok.getContent()));
		}
		if (method == HttpMethod.GET && (path.equals("/members/me/membership-orders/pending")
				|| path.equals("/reception/members/{memberId}/membership-orders/pending"))) {
			operation.getResponses().addApiResponse("204", new ApiResponse().description("No pending order."));
		}
		if (method == HttpMethod.POST && (path.equals("/members/me/membership-orders")
				|| path.equals("/reception/members/{memberId}/membership-orders")
				|| path.equals("/reception/members/{memberId}/cash-payments"))) {
			operation.getResponses().addApiResponse("409", reference("MembershipOrderConflict"));
		}
		if (method == HttpMethod.POST && (path.equals("/members/me/course-orders")
				|| path.equals("/reception/members/{memberId}/course-orders"))) {
			ApiResponse ok = operation.getResponses().get("200");
			operation.getResponses().addApiResponse("201",
				new ApiResponse().description("Created").content(ok.getContent()));
			operation.getResponses().addApiResponse("404", reference("NotFound"));
		}
		if (method == HttpMethod.POST && (path.equals("/course-runs/{runId}/waitlist")
				|| path.equals("/course-waitlists/{waitlistId}/accept"))) {
			ApiResponse ok = operation.getResponses().get("200");
			operation.getResponses().addApiResponse("201",
				new ApiResponse().description("Created").content(ok.getContent()));
		}
		if (method == HttpMethod.GET && path.equals("/manager/members/{memberId}/coach-assignment")) {
			operation.getResponses().addApiResponse("204", new ApiResponse().description("No active Coach assignment found."));
		}
		if (method == HttpMethod.GET && (path.equals("/members/me/coach-assignment")
				|| path.equals("/coach/class-sessions/{sessionId}/members/{memberId}/training-results")
				|| path.equals("/coach/class-sessions/{sessionId}/members/{memberId}/feedback"))) {
			operation.getResponses().addApiResponse("204", new ApiResponse().description("No active or linked record found."));
		}
		if (method == HttpMethod.DELETE && path.equals("/members/me/membership-termination-requests/{requestId}")) {
			operation.getResponses().keySet().removeIf(status -> status.matches("2\\d\\d"));
			operation.getResponses().addApiResponse("204", new ApiResponse().description("Request withdrawn."));
		}
		if (method == HttpMethod.DELETE && path.equals("/members/me/membership-freeze-requests/{requestId}")) {
			operation.getResponses().keySet().removeIf(status -> status.matches("2\\d\\d"));
			operation.getResponses().addApiResponse("204", new ApiResponse().description("Request withdrawn."));
		}
		if (method == HttpMethod.POST && path.equals("/reception/membership-orders/{orderId}/cash")) {
			operation.getResponses().keySet().removeIf(status -> status.matches("2\\d\\d"));
		}
		if (method == HttpMethod.POST && (path.equals("/members/class-sessions/{sessionId}/bookings")
				|| path.equals("/reception/members/{memberId}/bookings"))) {
			operation.getResponses().addApiResponse("409", reference("BookingConflict"));
		}
	}

	private Schema<?> problemSchema() {
		return new ObjectSchema()
			.addProperty("title", new StringSchema())
			.addProperty("status", new IntegerSchema().format("int32"))
			.addProperty("detail", new StringSchema())
			.addProperty("instance", new StringSchema().format("uri-reference"))
			.addProperty("code", new StringSchema())
			.addProperty("errors", new ObjectSchema().additionalProperties(
				new ArraySchema().items(new StringSchema())))
			.addProperty("affectedSessionIds", new ArraySchema().items(new StringSchema().format("uuid")))
			.addProperty("requiredBenefitCodes", new ArraySchema().items(new StringSchema()))
			.addProperty("missingBenefitCodes", new ArraySchema().items(new StringSchema()))
			.addProperty("affectedSessions", new ArraySchema().items(new ObjectSchema()
				.addProperty("sessionId", new StringSchema().format("uuid"))
				.addProperty("classId", new StringSchema().format("uuid"))
				.addProperty("className", new StringSchema())
				.addProperty("startTime", new StringSchema().format("date-time"))
				.addProperty("endTime", new StringSchema().format("date-time"))))
			.addProperty("traceId", new StringSchema())
			.required(List.of("title", "status", "detail", "instance", "code"));
	}

	private Schema<?> membershipBenefitRequiredProblemSchema() {
		Schema<?> schema = problemSchema();
		schema.addProperty("code", new StringSchema()._enum(List.of("MEMBERSHIP_BENEFIT_REQUIRED")));
		schema.required(List.of("title", "status", "detail", "instance", "code", "requiredBenefitCodes",
			"missingBenefitCodes"));
		return schema;
	}

	private Schema<?> unauthorizedProblemSchema() {
		Schema<?> schema = problemSchema();
		schema.addProperty("code", new StringSchema()._enum(UNAUTHORIZED_CODES));
		return schema;
	}

	private Schema<?> ordinaryBookingConflictProblemSchema() {
		Schema<?> schema = problemSchema();
		schema.addProperty("code", new StringSchema()
			.not(new StringSchema()._enum(List.of("MEMBERSHIP_BENEFIT_REQUIRED"))));
		return schema;
	}

	private void addBookingConflictResponse(Components components) {
		Schema<?> schema = new ComposedSchema()
			.addOneOfItem(new Schema<>().$ref("#/components/schemas/OrdinaryBookingConflictProblem"))
			.addOneOfItem(new Schema<>().$ref("#/components/schemas/MembershipBenefitRequiredProblem"));
		components.addResponses("BookingConflict", new ApiResponse()
			.description("A booking conflict, including MEMBERSHIP_BENEFIT_REQUIRED when required Benefits are missing.")
			.content(new Content().addMediaType("application/problem+json",
				new io.swagger.v3.oas.models.media.MediaType().schema(schema))));
	}

	private void addErrorResponse(Components components, String name, String description) {
		addErrorResponse(components, name, description, PROBLEM_SCHEMA);
	}

	private void addErrorResponse(Components components, String name, String description, String schemaRef) {
		components.addResponses(name, new ApiResponse().description(description)
			.content(new Content().addMediaType("application/problem+json",
				new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref(schemaRef)))));
	}

	private ApiResponse reference(String name) {
		return new ApiResponse().$ref("#/components/responses/" + name);
	}

	private String constraint(String path, boolean publicOperation) {
		if (publicOperation) {
			if (path.startsWith("/courses") || path.startsWith("/course-runs")) {
				return "Public course catalog; only PUBLISHED courses and OPEN_FOR_REGISTRATION runs are returned.";
			}
			return path.equals("/payments/sepay/webhook")
				? "Public SePay provider webhook; authenticated by the configured provider API key."
				: "Public authentication operation.";
		}
		if (path.equals("/reception/members/{memberId}/receipts")) {
			return "Requires MANAGER or RECEPTIONIST role; member access is role-scoped.";
		}
		if (path.startsWith("/manager/")) {
			if (path.contains("coach-assignment")) {
				return "Requires MANAGER role; PERSONAL_COACHING Benefit is required for assignment eligibility.";
			}
			return "Requires MANAGER role.";
		}
		if (path.startsWith("/reception/")) return "Requires RECEPTIONIST role; member access is role-scoped.";
		if (path.startsWith("/coach/")) {
			if (path.contains("training-plans")) {
				return "Requires COACH role; TRAINING_PLAN Benefit and ownership-scoped access. Session access is ownership-scoped.";
			}
			if (path.contains("training-results")) {
				return "Requires COACH role; TRAINING_RESULT Benefit and ownership-scoped access. Session access is ownership-scoped.";
			}
			if (path.contains("feedback")) {
				return "Requires COACH role; TRAINING_FEEDBACK Benefit and ownership-scoped access. Session access is ownership-scoped.";
			}
			if (path.contains("coach-assignments") || path.contains("coach-assignment")) {
				return "Requires COACH role; PERSONAL_COACHING Benefit and ownership-scoped access. Session access is ownership-scoped.";
			}
			return "Requires COACH role; session access is ownership-scoped.";
		}
		if (path.startsWith("/members/")) return "Requires MEMBER role; resource ownership is limited to the authenticated member.";
		if (path.startsWith("/membership-offers")) return "Public catalog; only ACTIVE membership offers are returned.";
		return "Requires an authenticated account; access is role- and ownership-scoped.";
	}
}
