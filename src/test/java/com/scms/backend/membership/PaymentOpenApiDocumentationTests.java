package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.scms.backend.config.OpenApiConfiguration;
import java.io.InputStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = { PaymentController.class, SepayController.class },
	excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({ OpenApiConfiguration.class, SpringDocConfiguration.class,
	SpringDocSecurityConfiguration.class, SpringDocWebMvcConfiguration.class })
class PaymentOpenApiDocumentationTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean PaymentService paymentService;
	@MockitoBean SepayService sepayService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void revenueRangeAndWebhookSecurityAreDocumented() throws Exception {
		JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		JsonNode report = document.at("/paths/~1manager~1reports~1membership-revenue/get");

		assertThat(report.path("description").asText()).contains("MANAGER", "UTC", "[from,to)");
		assertParameter(report, "from", "2026-10-07T17:00:00Z");
		assertParameter(report, "to", "2026-10-08T17:00:00Z");
		assertThat(report.path("security").isMissingNode()).isTrue();
		JsonNode revenueProperties = document.at("/components/schemas/RevenueReportResponse/properties");
		assertThat(revenueProperties.has("paidRevenue")).isTrue();
		assertThat(revenueProperties.has("completedRefundAmount")).isTrue();
		assertThat(revenueProperties.has("netRevenue")).isTrue();
		assertThat(document.at("/paths/~1payments~1sepay~1webhook/post/security")).isEmpty();
		assertThat(document.at("/paths/~1manager~1reports~1membership-revenue/get/responses/200/content")
			.isEmpty()).isFalse();
	}

	@Test
	void sepayResponseDocumentsManualFallbackContract() throws Exception {
		JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		JsonNode properties = document.at("/components/schemas/SepayPaymentResponse/properties");

		assertThat(properties.has("transferContent")).isTrue();
		assertThat(properties.has("bankCode")).isTrue();
		assertThat(properties.has("bankAccountNumber")).isTrue();
		assertThat(properties.has("bankAccountName")).isTrue();
		assertThat(properties.has("webhookApiKey")).isFalse();
		assertThat(properties.has("secretKey")).isFalse();
		assertThat(properties.has("authorization")).isFalse();
		assertThat(properties.has("credential")).isFalse();
	}

	@Test
	void staticStaffMembershipHistoryContractMatchesTheDedicatedResponse() throws Exception {
		try (InputStream stream = getClass().getResourceAsStream("/openapi/api-01-staff-membership-history.yaml")) {
			assertThat(stream).isNotNull();
			JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(stream);
			assertStaffMembershipOperation(document, "/reception/members/{memberCode}/memberships", "memberCode");
			assertStaffMembershipOperation(document, "/manager/members/{memberAccountId}/memberships", "memberAccountId");
			JsonNode badRequest = document.at("/paths/~1manager~1members~1{memberAccountId}~1memberships/get/responses/400");
			assertThat(badRequest.path("$ref").asText()).isEqualTo("#/components/responses/BadRequest");
			assertThat(document.at("/components/responses/BadRequest/content/application~1problem+json/schema/$ref")
				.asText()).isEqualTo("#/components/schemas/ApiProblem");
			JsonNode schema = document.at("/components/schemas/StaffMembershipResponse");
			assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
			assertThat(schema.path("properties").properties().stream().map(java.util.Map.Entry::getKey).toList())
				.containsExactlyInAnyOrder("membershipId", "planCode", "offerName", "status", "startsAt", "endsAt",
					"benefitCodes", "benefits");
			assertThat(schema.at("/properties/status/enum").toString())
				.isEqualTo("[\"RESERVED\",\"ACTIVE\",\"EXPIRED\",\"TERMINATED\",\"CANCELLED\"]");
		}
	}

	private void assertParameter(JsonNode operation, String name, String example) {
		JsonNode parameter = StreamSupport.stream(operation.path("parameters").spliterator(), false)
			.filter(node -> node.path("name").asText().equals(name))
			.findFirst()
			.orElseThrow();
		assertThat(parameter.path("description").asText()).contains("UTC", "half-open");
		assertThat(parameter.path("example").asText()).isEqualTo(example);
	}

	private void assertStaffMembershipOperation(JsonNode document, String path, String parameterName) {
		JsonNode operation = document.path("paths").path(path).path("get");
		assertThat(operation.path("parameters").findValuesAsText("name")).contains(parameterName);
		String responseReference = operation.at("/responses/200/$ref").asText();
		JsonNode response = document.at(responseReference.substring(1));
		assertThat(response.at("/content/application~1json/schema/items/$ref").asText())
			.isEqualTo("#/components/schemas/StaffMembershipResponse");
		assertThat(operation.path("responses").has("404")).isTrue();
	}
}
