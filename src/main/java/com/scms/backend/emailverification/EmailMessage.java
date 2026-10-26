package com.scms.backend.emailverification;

import java.util.List;

record EmailMessage(String from, List<String> to, String subject, String html, String text) {
	EmailMessage {
		to = List.copyOf(to);
	}
}
