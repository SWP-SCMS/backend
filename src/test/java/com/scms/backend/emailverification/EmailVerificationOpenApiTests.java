package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class EmailVerificationOpenApiTests {
	@Test
	@SuppressWarnings("unchecked")
	void manualActivationSchemaAndListParametersMatchImplementation() throws Exception {
		try(InputStream source=getClass().getResourceAsStream("/openapi/email-verification.yaml")){
			assertThat(source).isNotNull();String yaml=new String(source.readAllBytes(),StandardCharsets.UTF_8);
			Map<String,Object> document=new Yaml().load(yaml);assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String,Object> paths=(Map<String,Object>)document.get("paths");
			for(String path:List.of("/reception/email-verification/manual-activation-requests",
					"/manager/email-verification/manual-activation-requests")){
				Map<String,Object> operation=(Map<String,Object>)((Map<String,Object>)paths.get(path)).get("get");
				assertThat(operation.get("parameters").toString()).contains("ManualActivationStatus","Page","Size");
			}
			Map<String,Object> components=(Map<String,Object>)document.get("components");
			Map<String,Object> schemas=(Map<String,Object>)components.get("schemas");
			Map<String,Object> response=(Map<String,Object>)schemas.get("ManualActivationResponse");
			Map<String,Object> properties=(Map<String,Object>)response.get("properties");
			assertThat(properties).containsKeys("reasonCode","reasonDetail","verificationMethod","evidenceNote",
				"requestedByName","requestedByRole","reviewedByAccountId","decisionNote");
			assertThat(((Map<String,Object>)((Map<String,Object>)components.get("parameters")).get("Size"))
				.toString()).contains("maximum=100");
		}
	}
}
