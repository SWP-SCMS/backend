package com.scms.backend.course;

import com.scms.backend.account.*;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.member.LocalProfileImageStorage;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CourseImageController {
    private final LocalProfileImageStorage storage;
    private final AccountRepository accounts;

    CourseImageController(AccountRepository accounts,
            @Value("${scms.course-images.directory:./data/course-images}") String directory) {
        this.accounts = accounts;
        this.storage = new LocalProfileImageStorage(directory, 5L * 1024 * 1024);
    }

    @PostMapping(path = "/manager/courses/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse upload(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) {
        if (!accounts.existsByIdAndRoleAndStatus(UUID.fromString(jwt.getSubject()), AccountRole.MANAGER, AccountStatus.ACTIVE)) {
            throw new InvalidAuthenticatedAccountException();
        }
        var stored = storage.store(file);
        return new ImageResponse(stored.url().replace("/profile-images/", "/course-images/"));
    }

    record ImageResponse(String imageUrl) {}
}
