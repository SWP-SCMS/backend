package com.scms.backend.emailverification;

record EmailDeliveryResult(String providerMessageId) {
	static EmailDeliveryResult accepted(String providerMessageId) {
		return new EmailDeliveryResult(providerMessageId);
	}
}
