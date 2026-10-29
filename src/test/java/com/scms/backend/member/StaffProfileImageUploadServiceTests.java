package com.scms.backend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.identity.memberprofile.MemberProfileService;
import com.scms.backend.identity.receptionmember.ReceptionMemberProfileService;
import com.scms.backend.identity.staffprofile.StaffProfileResponse;
import com.scms.backend.identity.staffprofile.StaffProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

class StaffProfileImageUploadServiceTests {

	@Test
	void replacesImageAndDeletesOldFileOnlyAfterCommit() throws Exception {
		Fixture fixture = fixture();
		when(fixture.staffService.replaceProfileImage(fixture.accountId, fixture.stored.url()))
			.thenReturn("/profile-images/old.jpg");
		when(fixture.staffService.getProfile(fixture.accountId)).thenReturn(response(fixture));

		TransactionSynchronizationManager.initSynchronization();
		try {
			StaffProfileResponse response = fixture.service.uploadForStaff(fixture.accountId, image());
			assertThat(response.profileImageUrl()).isEqualTo(fixture.stored.url());
			verify(fixture.storage, Mockito.never()).delete(Mockito.anyString());
			TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			verify(fixture.storage).delete("/profile-images/old.jpg");
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void deletesNewFileWhenProfilePersistenceFails() throws Exception {
		Fixture fixture = fixture();
		when(fixture.staffService.replaceProfileImage(fixture.accountId, fixture.stored.url()))
			.thenThrow(new IllegalStateException("database failure"));

		assertThatThrownBy(() -> fixture.service.uploadForStaff(fixture.accountId, image()))
			.isInstanceOf(IllegalStateException.class);
		verify(fixture.storage).delete(fixture.stored.url());
	}

	private Fixture fixture() {
		ProfileImageStorage storage = Mockito.mock(ProfileImageStorage.class);
		MemberProfileService memberService = Mockito.mock(MemberProfileService.class);
		ReceptionMemberProfileService receptionService = Mockito.mock(ReceptionMemberProfileService.class);
		StaffProfileService staffService = Mockito.mock(StaffProfileService.class);
		ProfileImageUploadService service = new ProfileImageUploadService(storage, memberService, receptionService,
			staffService);
		UUID accountId = UUID.randomUUID();
		ProfileImageStorage.StoredImage stored = new ProfileImageStorage.StoredImage("/profile-images/new.jpg");
		when(storage.store(Mockito.any())).thenReturn(stored);
		return new Fixture(storage, staffService, service, accountId, stored);
	}

	private StaffProfileResponse response(Fixture fixture) {
		return new StaffProfileResponse(fixture.accountId, "Coach", "0901234567", "coach@example.com",
			LocalDate.of(1990, 1, 1), AccountRole.COACH, AccountStatus.ACTIVE, fixture.stored.url(), false,
			null, null, null);
	}

	private MultipartFile image() throws Exception {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", output);
		return new MockMultipartFile("file", "avatar.jpg", "image/jpeg", output.toByteArray());
	}

	private record Fixture(ProfileImageStorage storage, StaffProfileService staffService,
			ProfileImageUploadService service, UUID accountId, ProfileImageStorage.StoredImage stored) { }
}
