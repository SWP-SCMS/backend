package com.scms.backend.membership.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.membership.PaymentException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

class PaymentEvidenceServiceTests {

	private MockMultipartFile png() throws Exception {
		var bytes = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
		return new MockMultipartFile("file", "../../not-trusted.html", "text/html", bytes.toByteArray());
	}

	@Test
	void detectsImageFromBytesInsteadOfFilenameOrMimeType() throws Exception {
		var file = png();
		var image = PaymentEvidenceService.validate(file);
		assertThat(image.contentType()).isEqualTo("image/png");
		assertThat(image.bytes()).isEqualTo(file.getBytes());
	}

	@Test
	void rejectsEmptyFakeAndOversizedImages() {
		for (var file : List.of(new MockMultipartFile("file", new byte[0]), new MockMultipartFile("file", "fake.png", "image/png", "<script>alert(1)</script>".getBytes()), new MockMultipartFile("file", new byte[PaymentEvidenceService.MAX_BYTES + 1]))) {
			assertThatThrownBy(() -> PaymentEvidenceService.validate(file)).isInstanceOf(PaymentException.class);
		}
	}

	@Test
	void deniesNonStaffBeforeReadingOrWritingEvidence() throws Exception {
		var db = mock(JdbcTemplate.class);
		var service = new PaymentEvidenceService(db, mock(AccountRepository.class));
		var actor = UUID.randomUUID();
		var payment = UUID.randomUUID();
		var image = png();
		assertThatThrownBy(() -> service.upload(actor, payment, image)).isInstanceOf(PaymentException.class);
		assertThatThrownBy(() -> service.list(actor, payment)).isInstanceOf(PaymentException.class);
		assertThatThrownBy(() -> service.read(actor, payment, UUID.randomUUID())).isInstanceOf(PaymentException.class);
		verifyNoInteractions(db);
	}

	@Test
	void rejectsUploadAfterPaymentIsSettled() throws Exception {
		var actor = UUID.randomUUID();
		var payment = UUID.randomUUID();
		var accounts = mock(AccountRepository.class);
		var db = mock(JdbcTemplate.class);
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForList("select status from payments where id=? for update", String.class, payment)).thenReturn(List.of("PAID"));
		var service = new PaymentEvidenceService(db, accounts);
		var image = png();
		assertThatThrownBy(() -> service.upload(actor, payment, image)).isInstanceOfSatisfying(PaymentException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
	}
}
