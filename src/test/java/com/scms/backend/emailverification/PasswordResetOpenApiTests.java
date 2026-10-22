package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PasswordResetOpenApiTests {
	@Test
	@SuppressWarnings("unchecked")
	void contractDocumentsPublicThreeStepFlowWithoutCurrentPassword() throws Exception {
		try (InputStream source = getClass().getResourceAsStream("/openapi/mf-003-password-reset.yaml")) {
			assertThat(source).isNotNull();
			Map<String, Object> document = new Yaml().load(new String(source.readAllBytes(), StandardCharsets.UTF_8));
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			assertThat(paths).containsKeys("/auth/password-reset/request", "/auth/password-reset/verify",
				"/auth/password-reset/confirm");
			for (Object item : paths.values()) {
				Map<String, Object> post = (Map<String, Object>) ((Map<String, Object>) item).get("post");
				assertThat(post.get("security")).isEqualTo(java.util.List.of());
			}
			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			Map<String, Object> confirm = (Map<String, Object>) schemas.get("PasswordResetConfirmRequest");
			Map<String, Object> properties = (Map<String, Object>) confirm.get("properties");
			assertThat(properties).containsOnlyKeys("newPassword", "confirmPassword");
			assertThat(confirm.get("additionalProperties")).isEqualTo(false);
		}
	}
}
