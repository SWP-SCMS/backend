package com.scms.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.auth.AuthenticationController;
import com.scms.backend.auth.AuthenticationService;
import com.scms.backend.auth.RefreshTokenCookieService;
import com.scms.backend.auth.RegistrationController;
import com.scms.backend.auth.RegistrationService;
import com.scms.backend.manager.ResetPasswordService;
import com.scms.backend.manager.StaffAccountController;
import com.scms.backend.manager.StaffAccountCreateService;
import com.scms.backend.manager.StaffAccountService;
import com.scms.backend.manager.StaffStatusService;
import com.scms.backend.member.MemberProfileController;
import com.scms.backend.member.MemberProfileService;
import com.scms.backend.membership.MembershipOfferController;
import com.scms.backend.membership.MembershipOfferService;
import com.scms.backend.membership.MembershipOrderController;
import com.scms.backend.membership.MembershipOrderService;
import com.scms.backend.reception.ReceptionMemberController;
import com.scms.backend.reception.ReceptionMemberProfileService;
import com.scms.backend.reception.ReceptionMemberService;
import com.scms.backend.scheduling.BookingController;
import com.scms.backend.scheduling.BookingService;
import com.scms.backend.scheduling.DisciplineController;
import com.scms.backend.scheduling.DisciplineService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest(controllers = {
	AuthenticationController.class, RegistrationController.class, MemberProfileController.class,
	StaffAccountController.class, ReceptionMemberController.class, MembershipOfferController.class,
	MembershipOrderController.class, BookingController.class, DisciplineController.class
}, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({ OpenApiConfiguration.class, SpringDocConfiguration.class,
	SpringDocSecurityConfiguration.class, SpringDocWebMvcConfiguration.class })
class GeneratedOpenApiTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean AuthenticationService authenticationService;
	@MockitoBean RefreshTokenCookieService refreshTokenCookieService;
	@MockitoBean RegistrationService registrationService;
	@MockitoBean MemberProfileService memberProfileService;
	@MockitoBean StaffAccountService staffAccountService;
	@MockitoBean StaffAccountCreateService staffAccountCreateService;
	@MockitoBean StaffStatusService staffStatusService;
	@MockitoBean ResetPasswordService resetPasswordService;
	@MockitoBean ReceptionMemberService receptionMemberService;
	@MockitoBean ReceptionMemberProfileService receptionMemberProfileService;
	@MockitoBean MembershipOfferService membershipOfferService;
	@MockitoBean MembershipOrderService membershipOrderService;
	@MockitoBean BookingService bookingService;
	@MockitoBean DisciplineService disciplineService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void controllerSuccessResponsesHaveConcreteJavaTypes() throws Exception {
		ClassPathScanningCandidateComponentProvider scanner =
			new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
		List<String> wildcardMethods = new ArrayList<>();
		for (var candidate : scanner.findCandidateComponents("com.scms.backend")) {
			Class<?> controller = Class.forName(candidate.getBeanClassName());
			for (var method : controller.getDeclaredMethods()) {
				if (method.getGenericReturnType() instanceof ParameterizedType type
						&& type.getRawType().getTypeName().equals("org.springframework.http.ResponseEntity")
						&& type.getActualTypeArguments()[0] instanceof WildcardType) {
					wildcardMethods.add(controller.getSimpleName() + "." + method.getName());
				}
			}
		}
		assertThat(wildcardMethods).isEmpty();
	}

	@Test
	void generatedContractContainsRoutesSchemasAndSecurityAcrossModules() throws Exception {
		JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

		for (String path : List.of(
			"/auth/login", "/auth/refresh", "/auth/logout", "/auth/change-password", "/auth/register",
			"/members/me/profile", "/manager/staff-accounts", "/reception/members",
			"/membership-offers", "/members/me/membership-orders",
			"/members/class-sessions/{sessionId}/bookings", "/manager/disciplines")) {
			assertThat(document.at("/paths" + pointer(path)).isMissingNode()).as(path).isFalse();
		}

		assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();
		for (String publicOperation : List.of(
			"/paths/~1auth~1login/post", "/paths/~1auth~1refresh/post",
			"/paths/~1auth~1logout/post", "/paths/~1auth~1register/post")) {
			assertThat(document.at(publicOperation + "/security").isArray()).as(publicOperation).isTrue();
			assertThat(document.at(publicOperation + "/security")).as(publicOperation).isEmpty();
		}
		assertThat(document.at("/paths/~1members~1me~1profile/get/security").isMissingNode()).isTrue();
		assertThat(document.at("/paths/~1members~1me~1profile/get/description").asText())
			.contains("MEMBER", "ownership");
		assertThat(document.at("/paths/~1manager~1staff-accounts/get/description").asText())
			.contains("MANAGER");
		for (String status : List.of("400", "401", "403")) {
			JsonNode response = document.at("/paths/~1members~1me~1profile/get/responses/" + status);
			assertThat(response.isMissingNode()).as(status).isFalse();
			assertThat(response.path("$ref").asText()).startsWith("#/components/responses/");
		}
		assertThat(document.at("/components/responses/BadRequest/content/application~1problem+json")
			.isMissingNode()).isFalse();
		assertThat(document.at("/components/schemas/ApiProblem/required").toString())
			.contains("title", "status", "detail", "instance", "code");
		assertThat(document.at("/paths/~1auth~1login/post/responses/403").isMissingNode()).isTrue();
		assertThat(document.at("/paths/~1manager~1staff-accounts/post/responses/409").isMissingNode()).isFalse();
		for (String createdOperation : List.of(
			"/paths/~1auth~1register/post", "/paths/~1manager~1staff-accounts/post",
			"/paths/~1reception~1members/post", "/paths/~1members~1me~1membership-orders/post",
			"/paths/~1members~1class-sessions~1{sessionId}~1bookings/post",
			"/paths/~1manager~1disciplines/post")) {
			assertThat(document.at(createdOperation + "/responses/201").isMissingNode())
				.as(createdOperation).isFalse();
		}
		assertThat(document.at("/paths/~1auth~1logout/post/responses/204").isMissingNode()).isFalse();

		assertSuccessSchema(document, "/paths/~1auth~1login/post/responses/200");
		assertSuccessSchema(document, "/paths/~1members~1me~1profile/get/responses/200");
		assertSuccessSchema(document, "/paths/~1manager~1staff-accounts/get/responses/200");
		assertSuccessSchema(document, "/paths/~1reception~1members~1search/get/responses/200");
		assertSuccessSchema(document, "/paths/~1membership-offers/get/responses/200");
		assertSuccessSchema(document, "/paths/~1members~1class-sessions~1{sessionId}~1bookings/post/responses/201");
		assertSuccessSchema(document, "/paths/~1manager~1disciplines/get/responses/200");
		assertThat(document.at("/components/schemas/BookingResponse/properties/status/enum")).isNotEmpty();
	}

	private void assertSuccessSchema(JsonNode document, String responsePointer) {
		JsonNode content = document.at(responsePointer + "/content");
		JsonNode schema = content.isObject() && content.elements().hasNext()
			? content.elements().next().path("schema") : content;
		assertThat(schema.isMissingNode() || schema.isEmpty()).as(responsePointer).isFalse();
	}

	private String pointer(String path) {
		return "/" + path.replace("~", "~0").replace("/", "~1");
	}
}
