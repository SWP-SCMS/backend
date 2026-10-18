package com.scms.backend.membership;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class PaymentControllerTests {

	private MockMvc mockMvc;
	private PaymentService service;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(PaymentService.class);
		Jwt jwt = Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject(UUID.randomUUID().toString())
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(300))
			.build();
		HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
					&& parameter.getParameterType() == Jwt.class;
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest,
					org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
				return jwt;
			}
		};
		mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(service))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(jwtResolver)
			.build();
	}

	@Test
	void exposesReconciliationQueueEndpoint() throws Exception {
		mockMvc.perform(get("/payments/reconciliation-queue"))
			.andExpect(status().isOk());
	}

	@Test
	void exposesMembershipOrderCancellationEndpoint() throws Exception {
		mockMvc.perform(patch("/membership-orders/{orderId}/cancel", UUID.randomUUID())
			.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.content("{\"reason\":\"Customer no longer wants this order\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void exposesReceptionistMemberReceiptListEndpoint() throws Exception {
		mockMvc.perform(get("/reception/members/{memberId}/receipts", "MB-100001"))
			.andExpect(status().isOk());
	}

	@Test
	void exposesReceptionistMemberMembershipHistoryEndpoint() throws Exception {
		mockMvc.perform(get("/reception/members/{memberCode}/memberships", "MB-100001"))
			.andExpect(status().isOk());
	}

	@Test
	void exposesManagerMemberMembershipHistoryEndpoint() throws Exception {
		mockMvc.perform(get("/manager/members/{memberAccountId}/memberships", UUID.randomUUID()))
			.andExpect(status().isOk());
	}

	@Test
	void malformedManagerMemberIdReturnsProblemDetail() throws Exception {
		mockMvc.perform(get("/manager/members/{memberAccountId}/memberships", "not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Malformed request"))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}
}
