package com.scms.backend.emailverification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
class SmtpEmailProvider implements EmailProvider {
	private final JavaMailSender sender; private final EmailVerificationProperties properties;
	private final String from; private final String host; private final String supportContact;
	private final String template;
	SmtpEmailProvider(JavaMailSender sender, EmailVerificationProperties properties,
			@Value("${scms.email.from:}") String from,@Value("${spring.mail.host:}") String host,
			@Value("${scms.email.support-contact:support@scms.vn}") String supportContact){
		this.sender=sender;this.properties=properties;this.from=from;this.host=host;
		this.supportContact=supportContact;this.template=loadTemplate();}
	@PostConstruct void validate(){if(properties.isEnabled()&&(from.isBlank()||host.isBlank()))
		throw new IllegalStateException("SMTP host and sender are required when email verification is enabled");}
	@Override public void sendVerificationOtp(String recipient,String recipientName,String otp,long expiresInMinutes){
		if(otp==null||!otp.matches("^[0-9]{6}$"))throw new IllegalArgumentException("OTP must contain exactly six digits");
		String name=recipientName==null||recipientName.isBlank()?"bạn":recipientName.trim();
		String plain="Xin chào "+name+",\n\nMã xác thực tài khoản SCMS của bạn là: "+otp
			+"\nMã có hiệu lực trong "+expiresInMinutes+" phút. Không chia sẻ mã này với bất kỳ ai."
			+"\n\nNếu bạn không thực hiện yêu cầu này, hãy bỏ qua email hoặc liên hệ "+supportContact+".";
		String html=template.replace("{{recipientName}}",escape(name)).replace("{{otpCode}}",otp)
			.replace("{{expiresInMinutes}}",Long.toString(expiresInMinutes))
			.replace("{{supportContact}}",escape(supportContact))
			.replace("{{currentYear}}",Integer.toString(Year.now().getValue()));
		try{
			MimeMessage message=sender.createMimeMessage();MimeMessageHelper helper=new MimeMessageHelper(message,true,StandardCharsets.UTF_8.name());
			helper.setFrom(from);helper.setTo(recipient);helper.setSubject("Mã xác thực tài khoản SCMS");helper.setText(plain,html);
			sender.send(message);
		}catch(MessagingException exception){throw new IllegalStateException("Could not create verification email",exception);}
	}
	private String loadTemplate(){try{return new ClassPathResource("templates/email/email-verification.html")
		.getContentAsString(StandardCharsets.UTF_8);}catch(IOException exception){throw new IllegalStateException("Email verification template is unavailable",exception);}}
	private String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
		.replace("\"","&quot;").replace("'","&#39;");}
}
