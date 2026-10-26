package com.scms.backend.emailverification;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
class EmailProviderRouter {
	private final Map<String, EmailProvider> providers;

	EmailProviderRouter(List<EmailProvider> providers) {
		this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
			EmailProvider::providerName, Function.identity()));
	}

	EmailDeliveryResult send(String provider, EmailDeliveryRequest request) {
		EmailProvider selected = providers.get(provider);
		if (selected == null) throw EmailDeliveryException.terminal("UNKNOWN_EMAIL_PROVIDER");
		return selected.send(request);
	}
}
