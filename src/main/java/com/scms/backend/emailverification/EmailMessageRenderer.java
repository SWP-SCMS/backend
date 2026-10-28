package com.scms.backend.emailverification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
class EmailMessageRenderer {
	static final String VERIFICATION = "EMAIL_VERIFICATION_OTP";
	static final String PASSWORD_RESET = "PASSWORD_RESET_OTP";
	static final String EMAIL_CHANGE = "ACCOUNT_EMAIL_CHANGE_OTP";

	private final EmailDeliveryProperties properties;
	private final String verificationTemplate = load("email-verification.html");
	private final String passwordResetTemplate = load("password-reset.html");
	private final String emailChangeTemplate = load("email-change.html");

	EmailMessageRenderer(EmailDeliveryProperties properties) { this.properties = properties; }

	EmailMessage render(String templateKey, String recipient, String recipientName, String otp,
			long expiresInMinutes, Instant createdAt) {
		if (otp == null || !otp.matches("^[0-9]{6}$"))
			throw new IllegalArgumentException("OTP must contain exactly six digits");
		String name = recipientName == null || recipientName.isBlank() ? "bạn" : recipientName.trim();
		String support = properties.getSupportContact();
		String subject;
		String plain;
		String template;
		if (PASSWORD_RESET.equals(templateKey)) {
			subject = "Mã OTP đặt lại mật khẩu SCMS";
			plain = "Xin chào " + name + ",\n\nMã OTP đặt lại mật khẩu SCMS của bạn là: " + otp
				+ "\nMã có hiệu lực trong " + expiresInMinutes + " phút. Không chia sẻ mã này với bất kỳ ai."
				+ "\n\nNếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email và liên hệ " + support + ".";
			template = passwordResetTemplate;
		} else if (EMAIL_CHANGE.equals(templateKey) || "STAFF_EMAIL_CHANGE_OTP".equals(templateKey)) {
			subject = "Xác minh email mới cho tài khoản SCMS";
			plain = "Xin chào " + name + ",\n\nMã xác minh email mới của tài khoản SCMS là: " + otp
				+ "\nMã có hiệu lực trong " + expiresInMinutes + " phút. Không chia sẻ mã này với bất kỳ ai."
				+ "\n\nNếu bạn không yêu cầu đổi email, hãy hủy yêu cầu và liên hệ " + support + ".";
			template = emailChangeTemplate;
		} else {
			subject = "Mã xác thực tài khoản SCMS";
			plain = "Xin chào " + name + ",\n\nMã xác thực tài khoản SCMS của bạn là: " + otp
				+ "\nMã có hiệu lực trong " + expiresInMinutes + " phút. Không chia sẻ mã này với bất kỳ ai."
				+ "\n\nNếu bạn không thực hiện yêu cầu này, hãy bỏ qua email hoặc liên hệ " + support + ".";
			template = verificationTemplate;
		}
		String html = template.replace("{{recipientName}}", escape(name)).replace("{{otpCode}}", otp)
			.replace("{{expiresInMinutes}}", Long.toString(expiresInMinutes))
			.replace("{{supportContact}}", escape(support))
			.replace("{{currentYear}}", Integer.toString(createdAt.atZone(ZoneOffset.UTC).getYear()));
		return new EmailMessage(properties.getFrom(), List.of(recipient), subject, html, plain);
	}

	private static String load(String name) {
		try {
			return new ClassPathResource("templates/email/" + name).getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Email template is unavailable", exception);
		}
	}

	private String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
			.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
