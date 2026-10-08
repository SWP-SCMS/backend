package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.config.OpenApiConfiguration;
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
		assertThat(document.at("/paths/~1payments~1sepay~1webhook/post/security")).isEmpty();
		assertThat(document.at("/paths/~1manager~1reports~1membership-revenue/get/responses/200/content")
			.isEmpty()).isFalse();
	}

	private void assertParameter(JsonNode operation, String name, String example) {
		JsonNode parameter = StreamSupport.stream(operation.path("parameters").spliterator(), false)
			.filter(node -> node.path("name").asText().equals(name))
			.findFirst()
			.orElseThrow();
		assertThat(parameter.path("description").asText()).contains("UTC", "half-open");
		assertThat(parameter.path("example").asText()).isEqualTo(example);
	}
}
