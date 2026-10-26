package com.scms.backend.emailverification;

import java.time.Duration;
import java.util.Locale;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scms.email")
public class EmailDeliveryProperties {
	private String provider = "smtp";
	private String from = "";
	private String supportContact = "support@scms.vn";
	private final Resend resend = new Resend();

	@PostConstruct
	void validate() {
		provider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
		if (!provider.equals("smtp") && !provider.equals("resend"))
			throw new IllegalStateException("SCMS_EMAIL_PROVIDER must be smtp or resend");
		if (provider.equals("resend") && (resend.apiKey.isBlank() || resend.from.isBlank()))
			throw new IllegalStateException("RESEND_API_KEY and RESEND_FROM are required when SCMS_EMAIL_PROVIDER=resend");
		if (resend.connectTimeout.isNegative() || resend.connectTimeout.isZero()
				|| resend.readTimeout.isNegative() || resend.readTimeout.isZero())
			throw new IllegalStateException("Resend timeouts must be positive");
	}

	public String senderFor(String deliveryProvider) {
		return "resend".equals(deliveryProvider) ? resend.from : from;
	}

	public String getProvider() { return provider; }
	public void setProvider(String value) { provider = value; }
	public String getFrom() { return from; }
	public void setFrom(String value) { from = value == null ? "" : value; }
	public String getSupportContact() { return supportContact; }
	public void setSupportContact(String value) { supportContact = value; }
	public Resend getResend() { return resend; }

	public static class Resend {
		private String apiKey = "";
		private String from = "";
		private String baseUrl = "https://api.resend.com";
		private Duration connectTimeout = Duration.ofSeconds(5);
		private Duration readTimeout = Duration.ofSeconds(5);

		public String getApiKey() { return apiKey; }
		public void setApiKey(String value) { apiKey = value == null ? "" : value.trim(); }
		public String getFrom() { return from; }
		public void setFrom(String value) { from = value == null ? "" : value.trim(); }
		public String getBaseUrl() { return baseUrl; }
		public void setBaseUrl(String value) { baseUrl = value; }
		public Duration getConnectTimeout() { return connectTimeout; }
		public void setConnectTimeout(Duration value) { connectTimeout = value; }
		public Duration getReadTimeout() { return readTimeout; }
		public void setReadTimeout(Duration value) { readTimeout = value; }
	}
}
