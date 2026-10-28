package com.scms.backend.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PersonNameOpenApiTests {
	@Test
	void staticContractsParseAndDocumentThePersonNameRule() throws Exception {
		for (String resource : List.of("/openapi/us02-member-registration.yaml",
				"/openapi/us03-member-profile.yaml", "/openapi/us05-manager-staff-account-list.yaml",
				"/openapi/us11-receptionist-member-profile.yaml", "/openapi/us15-receptionist-create-member.yaml",
				"/openapi/staff-self-profile.yaml")) {
			try (var stream = getClass().getResourceAsStream(resource)) {
				assertThat(stream).as(resource).isNotNull();
				Object document = new Yaml().load(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
				assertThat(document.toString()).as(resource).contains("Unicode letters");
			}
		}
	}
}
