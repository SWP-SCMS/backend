package com.scms.backend.emailverification.delivery;

public interface EmailProvider {

	String providerName();

	EmailDeliveryResult send(EmailDeliveryRequest request);
}
