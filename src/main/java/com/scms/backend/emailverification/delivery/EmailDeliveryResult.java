package com.scms.backend.emailverification.delivery;

record EmailDeliveryResult(String providerMessageId) {

	static EmailDeliveryResult accepted(String providerMessageId) {
		return new EmailDeliveryResult(providerMessageId);
	}
}
