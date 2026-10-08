package com.scms.backend.scheduling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scms.backend.common.error.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = RecurringScheduleErrorContractTests.TestController.class,
	excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ GlobalExceptionHandler.class, RecurringScheduleErrorContractTests.TestController.class })
class RecurringScheduleErrorContractTests {

	@Autowired MockMvc mockMvc;

	@Test
	void conflictDoesNotExposeFieldErrors() throws Exception {
		mockMvc.perform(get("/schedule-conflict"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SESSION_CAPACITY_EXCEEDS_ROOM"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@RestController
	static class TestController {
		@GetMapping("/schedule-conflict")
		void conflict() {
			throw RecurringScheduleException.capacityConflict();
		}
	}
}
