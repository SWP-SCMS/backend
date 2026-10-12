package com.scms.backend.support;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scms.initial-manager")
public record InitialManagerProperties(boolean enabled, String fullName, String phone, String email,
		LocalDate birthDate, String password) {

	public InitialManagerProperties {
		if (fullName != null) fullName = fullName.trim();
		if (phone != null) phone = phone.trim();
		if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
	}
}
