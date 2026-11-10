package com.scms.backend.membership.payment;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.membership.PaymentException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class PaymentEvidenceService {

	static final int MAX_BYTES = 4 * 1024 * 1024;

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	PaymentEvidenceService(JdbcTemplate db, AccountRepository accounts) {
		this.db = db;
		this.accounts = accounts;
		this.paymentEvidenceRepository = new PaymentEvidenceRepository(this.db);
	}

	record Evidence(UUID id, Instant createdAt) {
	}

	record Image(String contentType, byte[] bytes) {
	}

	private void authorize(UUID actor) {
		if (!accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE) && !accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)) {
			throw PaymentException.forbidden();
		}
	}

	@Transactional(readOnly = true)
	List<Evidence> list(UUID actor, UUID paymentId) {
		authorize(actor);
		return paymentEvidenceRepository.listFindPaymentEvidenceImages(paymentId);
	}

	@Transactional
	Evidence upload(UUID actor, UUID paymentId, MultipartFile file) {
		authorize(actor);
		Image image = validate(file);
		var payments = paymentEvidenceRepository.uploadFindPayments(paymentId);
		if (payments.isEmpty())
			throw PaymentException.notFound("Payment was not found");
		if (!"PENDING".equals(payments.getFirst()))
			throw PaymentException.conflict("Payment is already settled");
		String digest;
		try {
			digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image.bytes()));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
		var existing = paymentEvidenceRepository.findDuplicateEvidence(paymentId, digest);
		if (!existing.isEmpty())
			return existing.getFirst();
		Integer count = paymentEvidenceRepository.countEvidence(paymentId);
		if (count != null && count >= 10)
			throw PaymentException.validation("Tối đa 10 ảnh chứng từ cho mỗi giao dịch.");
		UUID id = UUID.randomUUID();
		return paymentEvidenceRepository.insertEvidence(id, paymentId, actor, image.contentType(), digest, image.bytes());
	}

	@Transactional(readOnly = true)
	Image read(UUID actor, UUID paymentId, UUID evidenceId) {
		authorize(actor);
		var images = paymentEvidenceRepository.readFindPaymentEvidenceImages(evidenceId, paymentId);
		if (images.isEmpty())
			throw PaymentException.notFound("Evidence was not found");
		return images.getFirst();
	}

	static Image validate(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
			throw PaymentException.validation("Chọn ảnh PNG hoặc JPEG, dung lượng tối đa 4 MB.");
		}
		try (var stream = file.getInputStream()) {
			byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
			if (bytes.length == 0 || bytes.length > MAX_BYTES)
				throw new IOException("Image size exceeded");
			try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
				var readers = ImageIO.getImageReaders(input);
				if (!readers.hasNext())
					throw new IOException("Invalid image");
				var reader = readers.next();
				try {
					reader.setInput(input);
					String format = reader.getFormatName();
					if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
						throw new IOException("Unsupported image");
					long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
					if (pixels <= 0 || pixels > 20_000_000)
						throw new IOException("Image dimensions exceeded");
					if (reader.read(0) == null)
						throw new IOException("Invalid image");
					return new Image(format.equalsIgnoreCase("png") ? "image/png" : "image/jpeg", bytes);
				} finally {
					reader.dispose();
				}
			}
		} catch (IOException | IllegalArgumentException exception) {
			throw PaymentException.validation("Ảnh không hợp lệ. Chọn PNG/JPEG tối đa 4 MB và 20 triệu điểm ảnh.");
		}
	}

	private final PaymentEvidenceRepository paymentEvidenceRepository;
}
