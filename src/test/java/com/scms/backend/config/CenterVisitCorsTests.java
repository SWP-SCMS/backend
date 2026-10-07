package com.scms.backend.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CorsFilter;

class CenterVisitCorsTests {

	@Test
	void preflightAllowsIdempotencyKeyHeader() throws Exception {
		SecurityConfiguration security = new SecurityConfiguration("http://localhost:5173");
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EmptyController())
			.addFilters(new CorsFilter(security.corsConfigurationSource())).build();

		mockMvc.perform(options("/reception/center-visits")
			.header(HttpHeaders.ORIGIN, "http://localhost:5173")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Idempotency-Key"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
				containsString("Idempotency-Key")));
	}

	@RestController
	static class EmptyController {
	}
}
