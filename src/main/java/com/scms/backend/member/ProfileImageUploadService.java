package com.scms.backend.member;

import java.util.UUID;

import com.scms.backend.identity.memberprofile.MemberProfileResponse;
import com.scms.backend.identity.memberprofile.MemberProfileService;
import com.scms.backend.identity.receptionmember.ReceptionMemberProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileImageUploadService {

	private static final Logger logger = LoggerFactory.getLogger(ProfileImageUploadService.class);

	private final ProfileImageStorage storage;
	private final MemberProfileService memberProfileService;
	private final ReceptionMemberProfileService receptionMemberProfileService;

	ProfileImageUploadService(ProfileImageStorage storage, MemberProfileService memberProfileService,
			ReceptionMemberProfileService receptionMemberProfileService) {
		this.storage = storage;
		this.memberProfileService = memberProfileService;
		this.receptionMemberProfileService = receptionMemberProfileService;
	}

	@Transactional
	MemberProfileResponse uploadForMember(UUID accountId, MultipartFile file) {
		ProfileImageStorage.StoredImage stored = storage.store(file);
		try {
			String oldUrl = memberProfileService.replaceProfileImage(accountId, stored.url());
			MemberProfileResponse response = memberProfileService.getProfile(accountId);
			cleanupAfterCommit(stored.url(), oldUrl);
			return response;
		}
		catch (RuntimeException exception) {
			deleteQuietly(stored.url());
			throw exception;
		}
	}

	@Transactional
	MemberProfileResponse uploadForReceptionist(UUID receptionistAccountId, String memberId,
			MultipartFile file) {
		ProfileImageStorage.StoredImage stored = storage.store(file);
		try {
			String oldUrl = receptionMemberProfileService.replaceProfileImage(receptionistAccountId, memberId,
				stored.url());
			MemberProfileResponse response = receptionMemberProfileService.getProfile(receptionistAccountId, memberId);
			cleanupAfterCommit(stored.url(), oldUrl);
			return response;
		}
		catch (RuntimeException exception) {
			deleteQuietly(stored.url());
			throw exception;
		}
	}

	private void cleanupAfterCommit(String newUrl, String oldUrl) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status == STATUS_COMMITTED) {
						deleteQuietly(oldUrl);
					}
					else {
						deleteQuietly(newUrl);
					}
				}
			});
		}
		else {
			deleteQuietly(oldUrl);
		}
	}

	private void deleteQuietly(String url) {
		try {
			storage.delete(url);
		}
		catch (RuntimeException exception) {
			logger.warn("Could not clean up profile image {}", url, exception);
		}
	}
}
