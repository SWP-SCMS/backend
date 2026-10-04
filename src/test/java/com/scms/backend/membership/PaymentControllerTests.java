package com.scms.backend.membership;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

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

	@BeforeEach
	void setUp() {
		PaymentService service = org.mockito.Mockito.mock(PaymentService.class);
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
			.setCustomArgumentResolvers(jwtResolver)
			.build();
	}

	@Test
	void exposesReconciliationQueueEndpoint() throws Exception {
		mockMvc.perform(get("/payments/reconciliation-queue"))
			.andExpect(status().isOk());
	}
}
