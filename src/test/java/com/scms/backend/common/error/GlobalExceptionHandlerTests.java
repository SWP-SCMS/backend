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

import com.scms.backend.manager.StaffAccountNotFoundException;
import com.scms.backend.manager.StaffStatusConflictException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
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

@WebMvcTest(controllers = GlobalExceptionHandlerTests.TestController.class,
	excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
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
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value("One or more request fields are invalid."))
			.andExpect(jsonPath("$.instance").value("/validate"))
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
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value("The request body is missing or malformed."))
			.andExpect(jsonPath("$.instance").value("/validate"))
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void returnsProblemDetailForNotFoundAndConflictErrors() throws Exception {
		mockMvc.perform(get("/missing"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Staff account not found"))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("The requested staff account was not found."))
			.andExpect(jsonPath("$.instance").value("/missing"))
			.andExpect(jsonPath("$.code").value("STAFF_ACCOUNT_NOT_FOUND"))
			.andExpect(jsonPath("$.errors").doesNotExist());

		mockMvc.perform(get("/conflict"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Staff status conflict"))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Status cannot change."))
			.andExpect(jsonPath("$.instance").value("/conflict"))
			.andExpect(jsonPath("$.code").value("STAFF_STATUS_CONFLICT"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void hidesInternalDetailsForUnexpectedErrors() throws Exception {
		mockMvc.perform(get("/fail"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Internal server error"))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
			.andExpect(jsonPath("$.instance").value("/fail"))
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.errors").doesNotExist())
			.andExpect(content().string(not(containsString("sensitive implementation detail"))));
	}

	@Test
	void includesTraceIdWhenPresentInMdc() throws Exception {
		try {
			mockMvc.perform(get("/trace-fail"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.traceId").value("trace-123"));
		}
		finally {
			MDC.clear();
		}
	}

	@Test
	void preservesFrameworkClientErrorStatuses() throws Exception {
		mockMvc.perform(put("/validate"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Method Not Allowed"))
			.andExpect(jsonPath("$.status").value(405))
			.andExpect(jsonPath("$.detail").isNotEmpty())
			.andExpect(jsonPath("$.instance").value("/validate"))
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
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

		@GetMapping("/trace-fail")
		void traceFail() {
			MDC.put("traceId", "trace-123");
			throw new IllegalStateException("sensitive implementation detail");
		}

		@GetMapping("/missing")
		void missing() {
			throw new StaffAccountNotFoundException();
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new StaffStatusConflictException("Status cannot change.");
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
