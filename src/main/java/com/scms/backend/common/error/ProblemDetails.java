package com.scms.backend.common.error;

import java.net.URI;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

	private ProblemDetails() {
	}

	public static ProblemDetail create(HttpStatus status, String title, String detail, String code,
			String requestPath) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("code", code);
		return complete(problem, requestPath);
	}

	static ProblemDetail complete(ProblemDetail problem, String requestPath) {
		if (problem.getInstance() == null && requestPath != null) {
			problem.setInstance(URI.create(requestPath));
		}
		if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
			problem.setProperty("code", stableCode(problem));
		}
		String traceId = MDC.get("traceId");
		if (traceId != null && !traceId.isBlank()) {
			problem.setProperty("traceId", traceId);
		}
		return problem;
	}

	private static String stableCode(ProblemDetail problem) {
		String source = problem.getTitle();
		if (source == null || source.isBlank()) {
			source = HttpStatus.valueOf(problem.getStatus()).getReasonPhrase();
		}
		return source.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
	}
}
