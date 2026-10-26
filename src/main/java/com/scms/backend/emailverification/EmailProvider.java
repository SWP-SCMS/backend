package com.scms.backend.emailverification;

public interface EmailProvider {
	String providerName();
	EmailDeliveryResult send(EmailDeliveryRequest request);
}
