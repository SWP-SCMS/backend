package com.scms.backend.common.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTests.TestController.class)
class GlobalExceptionHandlerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsProblemDetailForValidationErrors() throws Exception {
		mockMvc.perform(post("/validate")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.name[0]").value("must not be blank"));
	}

	@Test
	void returnsProblemDetailForMalformedRequests() throws Exception {
		mockMvc.perform(post("/validate")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Malformed request"))
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void hidesInternalDetailsForUnexpectedErrors() throws Exception {
		mockMvc.perform(get("/fail"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Internal server error"))
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(content().string(not(containsString("sensitive implementation detail"))));
	}

	@Test
	void preservesFrameworkClientErrorStatuses() throws Exception {
		mockMvc.perform(put("/validate"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@RestController
	public static class TestController {

		@PostMapping("/validate")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/fail")
		void fail() {
			throw new IllegalStateException("sensitive implementation detail");
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
