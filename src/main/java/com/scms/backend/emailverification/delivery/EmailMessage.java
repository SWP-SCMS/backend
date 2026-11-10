package com.scms.backend.emailverification.delivery;

import java.util.List;

record EmailMessage(String from, List<String> to, String subject, String html, String text) {

	EmailMessage {
		to = List.copyOf(to);
	}
}
