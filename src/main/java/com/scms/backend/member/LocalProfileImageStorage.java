package com.scms.backend.member;

import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class LocalProfileImageStorage implements ProfileImageStorage, WebMvcConfigurer {

	private static final long DEFAULT_MAX_BYTES = 5L * 1024 * 1024;
	private static final String URL_PREFIX = "/profile-images/";

	private final Path directory;
	private final long maxBytes;

	public LocalProfileImageStorage(
			@Value("${scms.profile-images.directory:./data/profile-images}") String directory,
			@Value("${scms.profile-images.max-bytes:" + DEFAULT_MAX_BYTES + "}") long maxBytes) {
		if (maxBytes <= 0) {
			throw new IllegalArgumentException("Profile image max bytes must be positive");
		}
		this.directory = Path.of(directory).toAbsolutePath().normalize();
		this.maxBytes = maxBytes;
	}

	@Override
	public StoredImage store(MultipartFile file) {
		validateRequest(file);
		Path temporary = null;
		Path target = null;
		try {
			Files.createDirectories(directory);
			temporary = Files.createTempFile(directory, ".profile-image-", ".tmp");
			try (var input = file.getInputStream()) {
				Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
			}
			if (Files.size(temporary) > maxBytes) {
				throw invalid("must be at most " + maxBytes + " bytes");
			}

			String extension = validateImage(temporary);
			String filename = UUID.randomUUID() + "." + extension;
			target = directory.resolve(filename).normalize();
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
			}
			catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, target);
			}
			return new StoredImage(URL_PREFIX + filename);
		}
		catch (MemberProfileValidationException exception) {
			throw exception;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Could not store profile image", exception);
		}
		finally {
			if (temporary != null) {
				try {
					Files.deleteIfExists(temporary);
				}
				catch (IOException ignored) {
					// The database update must not be masked by best-effort temp cleanup.
				}
			}
		}
	}

	@Override
	public void delete(String profileImageUrl) {
		if (profileImageUrl == null || !profileImageUrl.startsWith(URL_PREFIX)) {
			return;
		}
		String filename = profileImageUrl.substring(URL_PREFIX.length());
		if (!filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) {
			return;
		}
		Path file = directory.resolve(filename).normalize();
		if (!file.getParent().equals(directory)) {
			return;
		}
		try {
			Files.deleteIfExists(file);
		}
		catch (IOException exception) {
			throw new IllegalStateException("Could not delete profile image", exception);
		}
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(URL_PREFIX + "**")
			.addResourceLocations(directory.toUri().toString());
	}

	private void validateRequest(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw invalid("must not be empty");
		}
		if (file.getSize() > maxBytes) {
			throw invalid("must be at most " + maxBytes + " bytes");
		}
	}

	private String validateImage(Path file) throws IOException {
		String format;
		try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
			if (input == null) {
				throw invalid("must be a valid JPEG or PNG image");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw invalid("must be a valid JPEG or PNG image");
			}
			ImageReader reader = readers.next();
			try {
				format = reader.getFormatName().toLowerCase(Locale.ROOT);
			}
			finally {
				reader.dispose();
			}
		}
		if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
			throw invalid("must be a JPEG or PNG image");
		}
		if (ImageIO.read(file.toFile()) == null) {
			throw invalid("must be a valid JPEG or PNG image");
		}
		return format.equals("png") ? "png" : "jpg";
	}

	private MemberProfileValidationException invalid(String message) {
		return new MemberProfileValidationException("file", message);
	}
}
