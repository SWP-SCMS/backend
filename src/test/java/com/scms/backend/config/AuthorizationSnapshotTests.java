package com.scms.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = SecurityProblemDetailTests.TestController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class, SecurityProblemDetailTests.TestController.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class AuthorizationSnapshotTests {

	@Autowired
	private SecurityFilterChain chain;

	@MockitoBean
	private ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void endpointRoleDecisionsMatchBaseline() throws Exception {
		var json = new ObjectMapper();
		var operations = new TreeSet<String>();
		try (var stream = getClass().getResourceAsStream("/cleanup/openapi-baseline.json")) {
			assertThat(stream).isNotNull();
			json.readTree(stream).path("paths").properties().forEach(path -> path.getValue().properties().stream().filter(method -> List.of("get", "post", "put", "patch", "delete", "head", "options").contains(method.getKey())).forEach(method -> operations.add(method.getKey().toUpperCase() + " " + path.getKey())));
		}
		assertThat(operations).hasSize(324);
		operations.add("OPTIONS /cleanup-unmapped");
		operations.add("GET /cleanup-unmapped");
		var authorization = chain.getFilters().stream().filter(AuthorizationFilter.class::isInstance).map(AuthorizationFilter.class::cast).findFirst().orElseThrow().getAuthorizationManager();
		Map<String, Boolean> decisions = new TreeMap<>();
		for (String operation : operations) {
			var parts = operation.split(" ", 2);
			String path = parts[1].replaceAll("\\{[^}]+}", "00000000-0000-0000-0000-000000000001");
			for (String role : List.of("ANONYMOUS", "AUTHENTICATED", "MEMBER", "COACH", "RECEPTIONIST", "MANAGER")) {
				Authentication actor = role.equals("ANONYMOUS") ? new AnonymousAuthenticationToken("cleanup", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) : UsernamePasswordAuthenticationToken.authenticated("actor", "unused", role.equals("AUTHENTICATED") ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role)));
				var request = new MockHttpServletRequest(parts[0], "/api/v1" + path);
				request.setContextPath("/api/v1");
				request.setServletPath(path);
				decisions.put(operation + "|" + role, authorization.authorize(() -> actor, request).isGranted());
			}
		}
		assertThat(decisions).hasSize(1956);
		try (var input = getClass().getResourceAsStream("/cleanup/authorization-baseline.json")) {
			assertThat(input).isNotNull();
			com.fasterxml.jackson.databind.JsonNode actual = json.valueToTree(decisions);
			assertThat(actual).isEqualTo(json.readTree(input));
		}
	}
}
