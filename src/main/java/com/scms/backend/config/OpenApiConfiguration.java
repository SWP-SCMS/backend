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

	@Bean
	OpenApiCustomizer apiContractCustomizer() {
		return openApi -> {
			Components components = openApi.getComponents() == null ? new Components() : openApi.getComponents();
			openApi.setComponents(components);
			components.addSchemas("ApiProblem", problemSchema());
			addErrorResponse(components, "BadRequest", "The request is invalid.");
			addErrorResponse(components, "Unauthorized", "Authentication failed or is required.");
			addErrorResponse(components, "Forbidden", "The authenticated account is not allowed to perform this operation.");
			addErrorResponse(components, "NotFound", "The requested resource was not found.");
			addErrorResponse(components, "Conflict", "The request conflicts with current resource state.");

			openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
				boolean publicOperation = operation.getSecurity() != null && operation.getSecurity().isEmpty();
				if (operation.getDescription() == null) {
					operation.setDescription(constraint(path, publicOperation));
				}
				operation.getResponses().addApiResponse("400", reference("BadRequest"));
				operation.getResponses().addApiResponse("401", reference("Unauthorized"));
				if (!publicOperation) {
					operation.getResponses().addApiResponse("403", reference("Forbidden"));
				}
				if (path.contains("{")) {
					operation.getResponses().addApiResponse("404", reference("NotFound"));
				}
				if (method != HttpMethod.GET) {
					operation.getResponses().addApiResponse("409", reference("Conflict"));
				}
				applyKnownResponseVariants(path, method, operation);
			}));
		};
	}

	private void applyKnownResponseVariants(String path, HttpMethod method, Operation operation) {
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
		if (method == HttpMethod.POST && path.equals("/reception/membership-orders/{orderId}/cash")) {
			operation.getResponses().keySet().removeIf(status -> status.matches("2\\d\\d"));
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
			.addProperty("traceId", new StringSchema())
			.required(List.of("title", "status", "detail", "instance", "code"));
	}

	private void addErrorResponse(Components components, String name, String description) {
		components.addResponses(name, new ApiResponse().description(description)
			.content(new Content().addMediaType("application/problem+json",
				new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref(PROBLEM_SCHEMA)))));
	}

	private ApiResponse reference(String name) {
		return new ApiResponse().$ref("#/components/responses/" + name);
	}

	private String constraint(String path, boolean publicOperation) {
		if (publicOperation) {
			return path.equals("/payments/sepay/webhook")
				? "Public SePay provider webhook; authenticated by the configured provider API key."
				: "Public authentication operation.";
		}
		if (path.equals("/reception/members/{memberId}/receipts")) {
			return "Requires MANAGER or RECEPTIONIST role; member access is role-scoped.";
		}
		if (path.startsWith("/manager/")) return "Requires MANAGER role.";
		if (path.startsWith("/reception/")) return "Requires RECEPTIONIST role; member access is role-scoped.";
		if (path.startsWith("/coach/")) return "Requires COACH role; session access is ownership-scoped.";
		if (path.startsWith("/members/")) return "Requires MEMBER role; resource ownership is limited to the authenticated member.";
		if (path.startsWith("/membership-offers")) return "Public catalog; only ACTIVE membership offers are returned.";
		return "Requires an authenticated account; access is role- and ownership-scoped.";
	}
}
