package com.scms.backend.member;

import com.scms.backend.identity.memberprofile.MemberProfileResponse;
import com.scms.backend.identity.staffprofile.StaffProfileResponse;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProfileImageUploadController {

	private final ProfileImageUploadService service;

	ProfileImageUploadController(ProfileImageUploadService service) {
		this.service = service;
	}

	@PostMapping(path = "/members/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<MemberProfileResponse> uploadMemberProfileImage(@AuthenticationPrincipal Jwt jwt,
			@RequestPart("file") MultipartFile file) {
		return ResponseEntity.ok(service.uploadForMember(accountId(jwt), file));
	}

	@PostMapping(path = "/reception/members/{memberId}/profile/image",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<MemberProfileResponse> uploadReceptionMemberProfileImage(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId, @RequestPart("file") MultipartFile file) {
		return ResponseEntity.ok(service.uploadForReceptionist(accountId(jwt), memberId, file));
	}

	@PostMapping(path = "/staff/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<StaffProfileResponse> uploadStaffProfileImage(@AuthenticationPrincipal Jwt jwt,
			@RequestPart("file") MultipartFile file) {
		return ResponseEntity.ok(service.uploadForStaff(accountId(jwt), file));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
