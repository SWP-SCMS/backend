package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AccountEmailChangeOpenApiTests {
	@Test
	@SuppressWarnings("unchecked")
	void contractExposesSharedLifecycleAndDoesNotAcceptEmailInProfilePatch() throws Exception {
		try (InputStream source = getClass().getResourceAsStream("/openapi/mf-012-account-email-change.yaml")) {
			assertThat(source).isNotNull();
			Map<String, Object> document = new Yaml().load(new String(source.readAllBytes(), StandardCharsets.UTF_8));
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			assertThat(((Map<String, Object>) paths.get("/accounts/me/email-change"))).containsKeys(
				"get", "post", "put", "delete");
			assertThat(paths).containsKeys("/accounts/me/email-change/verify", "/accounts/me/email-change/resend");
		}
		try (InputStream source = getClass().getResourceAsStream("/openapi/us03-member-profile.yaml")) {
			Map<String, Object> document = new Yaml().load(new String(source.readAllBytes(), StandardCharsets.UTF_8));
			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			Map<String, Object> patch = (Map<String, Object>) schemas.get("MemberProfilePatchRequest");
			assertThat((Map<String, Object>) patch.get("properties")).doesNotContainKey("email");
		}
	}
}
