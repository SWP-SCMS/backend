package com.scms.backend.emailverification;

import java.util.Locale;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scms.email")
public class EmailDeliveryProperties {
	private String provider = "smtp";
	private String from = "";
	private String smtpUsername = "";
	private boolean allowCustomFrom;
	private String recipientOverride = "";
	private boolean staging;
	private boolean production;
	private String supportContact = "support@scms.vn";

	@PostConstruct
	void validate() {
		provider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
		if (!provider.equals("smtp"))
			throw new IllegalStateException("SCMS_EMAIL_PROVIDER must be smtp");
		if ((staging || production) && (from.isBlank() || smtpUsername.isBlank()))
			throw new IllegalStateException("SMTP_FROM and SMTP_USERNAME are required in staging and production");
		if (!from.isBlank() && !smtpUsername.isBlank() && !allowCustomFrom && !from.equalsIgnoreCase(smtpUsername))
			throw new IllegalStateException("SMTP_FROM must equal SMTP_USERNAME unless SMTP_ALLOW_CUSTOM_FROM=true");
		if (staging && recipientOverride.isBlank())
			throw new IllegalStateException("SMTP_RECIPIENT_OVERRIDE is required in staging");
		if (production && !recipientOverride.isBlank())
			throw new IllegalStateException("SMTP_RECIPIENT_OVERRIDE cannot be enabled in production");
	}

	public String getProvider() { return provider; }
	public void setProvider(String value) { provider = value; }
	public String getFrom() { return from; }
	public void setFrom(String value) { from = value == null ? "" : value; }
	public String getSmtpUsername() { return smtpUsername; }
	public void setSmtpUsername(String value) { smtpUsername = value == null ? "" : value; }
	public boolean isAllowCustomFrom() { return allowCustomFrom; }
	public void setAllowCustomFrom(boolean value) { allowCustomFrom = value; }
	public String getRecipientOverride() { return recipientOverride; }
	public void setRecipientOverride(String value) { recipientOverride = value == null ? "" : value; }
	public void setStaging(boolean value) { staging = value; }
	public void setProduction(boolean value) { production = value; }
	String recipient(String original) { return staging && !production ? recipientOverride : original; }
	public String getSupportContact() { return supportContact; }
	public void setSupportContact(String value) { supportContact = value; }
}
