package com.scms.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FeaturePackageStructureTests {

	@Test
	void staffProfileBelongsToItsIdentityFeaturePackage() {
		for (String type : new String[] {
			"StaffProfileController",
			"StaffProfileService",
			"StaffProfilePatchRequest",
			"StaffProfileResponse"
		}) {
			assertDoesNotThrow(() -> Class.forName("com.scms.backend.identity.staffprofile." + type));
			assertThrows(ClassNotFoundException.class,
				() -> Class.forName("com.scms.backend.manager." + type));
		}
	}
}
