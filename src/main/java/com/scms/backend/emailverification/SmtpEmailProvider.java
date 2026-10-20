package com.scms.backend.emailverification;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class SmtpEmailProvider implements EmailProvider {
	private final JavaMailSender sender; private final EmailVerificationProperties properties;
	private final String from; private final String host;
	SmtpEmailProvider(JavaMailSender sender, EmailVerificationProperties properties,
			@Value("${scms.email.from:}") String from,@Value("${spring.mail.host:}") String host){
		this.sender=sender;this.properties=properties;this.from=from;this.host=host;}
	@PostConstruct void validate(){if(properties.isEnabled()&&(from.isBlank()||host.isBlank()))
		throw new IllegalStateException("SMTP host and sender are required when email verification is enabled");}
	@Override public void sendVerificationOtp(String recipient,String otp,long expiresInMinutes){
		SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(recipient);
		message.setSubject("SCMS email verification code");
		message.setText("Your SCMS verification code is "+otp+". It expires in "+expiresInMinutes+" minutes. Do not share this code.");
		sender.send(message);
	}
}
