package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailProviderTests {
	private JavaMailSender sender;
	private SmtpEmailProvider provider;

	@BeforeEach
	void setUp() {
		sender = mock(JavaMailSender.class);
		when(sender.createMimeMessage()).thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
		provider = new SmtpEmailProvider(sender, new EmailVerificationProperties(), "no-reply@scms.vn",
			"smtp.example.test", "hotro@scms.vn");
	}

	@Test
	void sendsVietnameseMultipartEmailAndEscapesUserControlledValues() throws Exception {
		provider.sendVerificationOtp("member@example.com", "An <script>alert(1)</script>", "123456", 10);

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(sender).send(captor.capture());
		MimeMessage message = captor.getValue();
		message.saveChanges();
		assertThat(message.getSubject()).isEqualTo("Mã xác thực tài khoản SCMS");
		assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("member@example.com");
		assertThat(find(message, "text/plain")).contains("123456", "10 phút", "An <script>");
		assertThat(find(message, "text/html")).contains("123456", "10 phút",
			"An &lt;script&gt;alert(1)&lt;/script&gt;").doesNotContain("An <script>");
	}

	@Test
	void rejectsOtpOutsideTheGeneratedSixDigitFormat() {
		assertThatThrownBy(() -> provider.sendVerificationOtp("member@example.com", "An", "12<script>", 10))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void sendsDedicatedPasswordResetTemplateWithSecurityWarning() throws Exception {
		provider.sendPasswordResetOtp("member@example.com", "An", "654321", 10);

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(sender).send(captor.capture());
		MimeMessage message = captor.getValue();
		message.saveChanges();
		assertThat(message.getSubject()).isEqualTo("Mã OTP đặt lại mật khẩu SCMS");
		assertThat(find(message, "text/plain")).contains("654321", "Nếu bạn không yêu cầu đặt lại mật khẩu");
		assertThat(find(message, "text/html")).contains("654321", "Nếu bạn không yêu cầu đặt lại mật khẩu");
	}

	@Test
	void sendsDedicatedEmailChangeTemplate() throws Exception {
		provider.sendEmailChangeOtp("new@example.com", "An", "654321", 10);
		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(sender).send(captor.capture());
		MimeMessage message = captor.getValue();message.saveChanges();
		assertThat(message.getSubject()).isEqualTo("Xác minh email mới cho tài khoản SCMS");
		assertThat(find(message,"text/plain")).contains("654321","Nếu bạn không yêu cầu đổi email");
		assertThat(find(message,"text/html")).contains("654321","Xác minh email mới");
	}

	private String find(Part part, String mimeType) throws Exception {
		Object content = part.getContent();
		if (content instanceof Multipart multipart) {
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				String nested = find(bodyPart, mimeType);
				if (nested != null) return nested;
			}
		}
		if (part.isMimeType(mimeType)) return content.toString();
		return null;
	}
}
