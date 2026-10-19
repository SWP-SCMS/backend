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

	@Test
	void centerVisitBelongsToItsFeaturePackage() {
		for (String type : new String[] {
			"CenterVisit",
			"CenterVisitController",
			"CenterVisitException",
			"CenterVisitPageResponse",
			"CenterVisitRepository",
			"CenterVisitRequest",
			"CenterVisitResponse",
			"CenterVisitService",
			"CheckoutSource"
		}) {
			assertDoesNotThrow(() -> Class.forName("com.scms.backend.centervisit." + type));
			assertThrows(ClassNotFoundException.class,
				() -> Class.forName("com.scms.backend.reception." + type));
		}
	}

	@Test
	void receptionMemberManagementBelongsToItsIdentityFeaturePackage() {
		for (String type : new String[] {
			"ReceptionMemberController",
			"ReceptionMemberCreateRequest",
			"ReceptionMemberNotFoundException",
			"ReceptionMemberProfileService",
			"ReceptionMemberSearchResponse",
			"ReceptionMemberService",
			"ReceptionMemberValidationException"
		}) {
			assertDoesNotThrow(() -> Class.forName("com.scms.backend.identity.receptionmember." + type));
			assertThrows(ClassNotFoundException.class,
				() -> Class.forName("com.scms.backend.reception." + type));
		}
	}
}
