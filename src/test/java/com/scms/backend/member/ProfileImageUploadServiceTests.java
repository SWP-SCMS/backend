package com.scms.backend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.scms.backend.identity.memberprofile.MemberProfileResponse;
import com.scms.backend.identity.memberprofile.MemberProfileService;
import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import com.scms.backend.identity.receptionmember.ReceptionMemberProfileService;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

class ProfileImageUploadServiceTests {

	@TempDir
	Path directory;

	@Test
	void storesDecodedJpegWithUuidFilenameAndIgnoresClientMetadata() throws Exception {
		LocalProfileImageStorage storage = new LocalProfileImageStorage(directory.toString(), 5 * 1024 * 1024);

		ProfileImageStorage.StoredImage stored = storage.store(image("client-name.png", "text/plain", "jpg"));

		assertThat(stored.url()).matches("/profile-images/[0-9a-f-]{36}\\.jpg");
		String filename = Path.of(stored.url()).getFileName().toString();
		assertThat(Files.exists(directory.resolve(filename))).isTrue();
		assertThat(filename).doesNotContain("client-name");
	}

	@Test
	void rejectsNonImageBytesEvenWhenMimeAndExtensionClaimPng() {
		LocalProfileImageStorage storage = new LocalProfileImageStorage(directory.toString(), 5 * 1024 * 1024);
		MultipartFile file = new MockMultipartFile("file", "spoof.png", "image/png", "not an image".getBytes());

		assertThatThrownBy(() -> storage.store(file))
			.isInstanceOf(MemberProfileValidationException.class)
			.hasMessageContaining("JPEG or PNG");
	}

	@Test
	void rejectsFilesAboveConfiguredLimit() {
		LocalProfileImageStorage storage = new LocalProfileImageStorage(directory.toString(), 4);
		MultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[5]);

		assertThatThrownBy(() -> storage.store(file))
			.isInstanceOf(MemberProfileValidationException.class)
			.hasMessageContaining("bytes");
	}

	@Test
	void deletesNewFileWhenProfilePersistenceFails() {
		ProfileImageStorage storage = Mockito.mock(ProfileImageStorage.class);
		MemberProfileService memberService = Mockito.mock(MemberProfileService.class);
		ReceptionMemberProfileService receptionService = Mockito.mock(ReceptionMemberProfileService.class);
		ProfileImageUploadService service = new ProfileImageUploadService(storage, memberService, receptionService);
		UUID accountId = UUID.randomUUID();
		ProfileImageStorage.StoredImage stored = new ProfileImageStorage.StoredImage("/profile-images/new.jpg");
		when(storage.store(Mockito.any())).thenReturn(stored);
		when(memberService.replaceProfileImage(accountId, stored.url()))
			.thenThrow(new IllegalStateException("database failure"));

		assertThatThrownBy(() -> service.uploadForMember(accountId, image("new.jpg", "image/jpeg", "jpg")))
			.isInstanceOf(IllegalStateException.class);

		verify(storage).delete(stored.url());
	}

	@Test
	void acceptsPngContentRegardlessOfClientFilename() throws Exception {
		LocalProfileImageStorage storage = new LocalProfileImageStorage(directory.toString(), 5 * 1024 * 1024);

		ProfileImageStorage.StoredImage stored = storage.store(image("client-name.jpg", "image/jpeg", "png"));

		assertThat(stored.url()).endsWith(".png");
	}

	@Test
	void deletesOldFileOnlyAfterTransactionCommit() throws Exception {
		ProfileImageStorage storage = Mockito.mock(ProfileImageStorage.class);
		MemberProfileService memberService = Mockito.mock(MemberProfileService.class);
		ReceptionMemberProfileService receptionService = Mockito.mock(ReceptionMemberProfileService.class);
		ProfileImageUploadService service = new ProfileImageUploadService(storage, memberService, receptionService);
		UUID accountId = UUID.randomUUID();
		ProfileImageStorage.StoredImage stored = new ProfileImageStorage.StoredImage("/profile-images/new.jpg");
		when(storage.store(Mockito.any())).thenReturn(stored);
		when(memberService.replaceProfileImage(accountId, stored.url())).thenReturn("/profile-images/old.jpg");
		when(memberService.getProfile(accountId)).thenReturn(new MemberProfileResponse(accountId, "MB-123", "Member",
			"0901234567", "member@example.com", java.time.LocalDate.of(2000, 1, 1),
			com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE,
			stored.url(), null, null, null));

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.uploadForMember(accountId, image("new.jpg", "image/jpeg", "jpg"));
			verify(storage, Mockito.never()).delete(Mockito.anyString());
			TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			verify(storage).delete("/profile-images/old.jpg");
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void deletesNewFileAfterTransactionRollbackAndKeepsOldFile() throws Exception {
		ProfileImageStorage storage = Mockito.mock(ProfileImageStorage.class);
		MemberProfileService memberService = Mockito.mock(MemberProfileService.class);
		ReceptionMemberProfileService receptionService = Mockito.mock(ReceptionMemberProfileService.class);
		ProfileImageUploadService service = new ProfileImageUploadService(storage, memberService, receptionService);
		UUID accountId = UUID.randomUUID();
		ProfileImageStorage.StoredImage stored = new ProfileImageStorage.StoredImage("/profile-images/new.jpg");
		when(storage.store(Mockito.any())).thenReturn(stored);
		when(memberService.replaceProfileImage(accountId, stored.url())).thenReturn("/profile-images/old.jpg");
		when(memberService.getProfile(accountId)).thenReturn(new MemberProfileResponse(accountId, "MB-123", "Member",
			"0901234567", "member@example.com", java.time.LocalDate.of(2000, 1, 1),
			com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE,
			stored.url(), null, null, null));

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.uploadForMember(accountId, image("new.jpg", "image/jpeg", "jpg"));
			TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
			verify(storage).delete(stored.url());
			org.mockito.Mockito.verify(storage, Mockito.never()).delete("/profile-images/old.jpg");
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private MultipartFile image(String filename, String contentType, String format) throws Exception {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, format, output);
		return new MockMultipartFile("file", filename, contentType, output.toByteArray());
	}
}
