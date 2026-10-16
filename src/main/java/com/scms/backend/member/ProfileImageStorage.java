package com.scms.backend.member;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorage {

	StoredImage store(MultipartFile file);

	void delete(String profileImageUrl);

	record StoredImage(String url) {
	}
}
