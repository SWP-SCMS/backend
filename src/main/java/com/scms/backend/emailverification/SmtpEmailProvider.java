package com.scms.backend.emailverification;

import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
class SmtpEmailProvider implements EmailProvider {
	private final JavaMailSender sender;
	private final EmailVerificationProperties verification;
	private final EmailDeliveryProperties delivery;
	private final String host;

	SmtpEmailProvider(JavaMailSender sender, EmailVerificationProperties verification,
			EmailDeliveryProperties delivery, @Value("${spring.mail.host:}") String host) {
		this.sender = sender;
		this.verification = verification;
		this.delivery = delivery;
		this.host = host;
	}

	@PostConstruct
	void validate() {
		if (verification.isEnabled() && delivery.getProvider().equals("smtp")
				&& (delivery.getFrom().isBlank() || host.isBlank()))
			throw new IllegalStateException("SMTP host and sender are required when SCMS_EMAIL_PROVIDER=smtp");
	}

	@Override
	public String providerName() { return "smtp"; }

	@Override
	public EmailDeliveryResult send(EmailDeliveryRequest request) {
		EmailMessage email = request.message();
		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(email.from());
			helper.setTo(email.to().toArray(String[]::new));
			helper.setSubject(email.subject());
			helper.setText(email.text(), email.html());
			sender.send(message);
			return EmailDeliveryResult.accepted(null);
		} catch (MessagingException | RuntimeException exception) {
			throw EmailDeliveryException.retryable("SMTP_DELIVERY_FAILED", null, exception);
		}
	}
}
