package com.scms.backend.emailverification;

import java.util.Locale;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scms.email")
public class EmailDeliveryProperties {
	private String provider = "smtp";
	private String from = "";
	private String supportContact = "support@scms.vn";

	@PostConstruct
	void validate() {
		provider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
		if (!provider.equals("smtp"))
			throw new IllegalStateException("SCMS_EMAIL_PROVIDER must be smtp");
	}

	public String getProvider() { return provider; }
	public void setProvider(String value) { provider = value; }
	public String getFrom() { return from; }
	public void setFrom(String value) { from = value == null ? "" : value; }
	public String getSupportContact() { return supportContact; }
	public void setSupportContact(String value) { supportContact = value; }
}
