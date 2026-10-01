package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class MembershipOrderOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us16OpenApiIsValidYamlAndDeclaresAllFourOperations() {
		try (InputStream source = getClass().getResourceAsStream("/openapi/us16-membership-order.yaml")) {
			assertThat(source).isNotNull();
			Map<String, Object> document = new Yaml().load(source);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> responseComponents = (Map<String, Object>) components.get("responses");
			assertThat(paths).hasSize(4);

			Map<String, Object> selfPostResponses = responses(paths,
				"/members/me/membership-orders", "post");
			Map<String, Object> selfPostConflict = resolveResponse(selfPostResponses, responseComponents, "409");
			assertThat((String) selfPostConflict.get("description"))
				.contains("ACTIVE_MEMBERSHIP_EXISTS", "PENDING_MEMBERSHIP_ORDER_EXISTS")
				.doesNotContain("MEMBER_NOT_ACTIVE");

			Map<String, Object> selfPendingResponses = responses(paths,
				"/members/me/membership-orders/pending", "get");
			assertThat(selfPendingResponses).doesNotContainKey("409");

			Map<String, Object> receptionPostResponses = responses(paths,
				"/reception/members/{memberId}/membership-orders", "post");
			assertThat((String) resolveResponse(receptionPostResponses, responseComponents, "409")
				.get("description")).contains("MEMBER_NOT_ACTIVE");

			Map<String, Object> receptionPendingResponses = responses(paths,
				"/reception/members/{memberId}/membership-orders/pending", "get");
			assertThat((String) resolveResponse(receptionPendingResponses, responseComponents, "409")
				.get("description")).contains("MEMBER_NOT_ACTIVE");
		}
		catch (java.io.IOException exception) {
			throw new AssertionError(exception);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> responses(Map<String, Object> paths, String path, String method) {
		Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
		assertThat(pathItem).containsKey(method);
		Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);
		return (Map<String, Object>) operation.get("responses");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveResponse(Map<String, Object> operationResponses,
			Map<String, Object> responseComponents, String status) {
		Map<String, Object> reference = (Map<String, Object>) operationResponses.get(status);
		assertThat(reference).isNotNull();
		String componentName = ((String) reference.get("$ref")).substring("#/components/responses/".length());
		return (Map<String, Object>) responseComponents.get(componentName);
	}
}
