package com.scms.backend.course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.scms.backend.account.*;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

class CourseImageControllerTests {
    @TempDir Path directory;
    @Test void uploadsImageAndRejectsInvalidFilesAndInactiveAccounts() throws Exception {
        var accounts = mock(AccountRepository.class);
        var id = UUID.randomUUID();
        var jwt = Jwt.withTokenValue("test").header("alg", "HS256").subject(id.toString()).build();
        var controller = new CourseImageController(accounts, directory.toString());
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        var file = new MockMultipartFile("file", "../../image.png", "image/png", out.toByteArray());
        assertThrows(InvalidAuthenticatedAccountException.class, () -> controller.upload(jwt, file));
        when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
        var response = controller.upload(jwt, file);
        assertTrue(response.imageUrl().matches("/course-images/[0-9a-f-]{36}\\.png"));
        assertArrayEquals(out.toByteArray(), Files.readAllBytes(directory.resolve(response.imageUrl().substring("/course-images/".length()))));
        assertThrows(MemberProfileValidationException.class, () -> controller.upload(jwt,
            new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes())));
        assertThrows(MemberProfileValidationException.class, () -> controller.upload(jwt,
            new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1])));
    }
}
