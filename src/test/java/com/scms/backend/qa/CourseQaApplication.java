package com.scms.backend.qa;

import com.scms.backend.ScmsBackendApplication;
import com.scms.backend.account.AccountRole;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

/**
 * Child JVM entry point. Only available on the test classpath.
 */
public final class CourseQaApplication {

	public static void main(String[] args) throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Path directory = Path.of(required("SCMS_QA_DIRECTORY"));
		String jdbc = required("SCMS_QA_JDBC_URL");
		if (!jdbc.startsWith("jdbc:postgresql://") || !required("SCMS_QA_DISPOSABLE").equals("true"))
			throw new IllegalArgumentException("Disposable QA database required");
		Files.createDirectories(directory);
		var app = new SpringApplicationBuilder(ScmsBackendApplication.class, Controls.class).profiles("test");
		var context = app.run("--server.address=127.0.0.1", "--server.port=" + System.getenv().getOrDefault("SCMS_QA_PORT", "0"), "--spring.datasource.url=" + jdbc, "--spring.datasource.username=" + required("SCMS_QA_DB_USER"), "--spring.datasource.password=" + required("SCMS_QA_DB_PASSWORD"), "--spring.flyway.url=" + jdbc, "--spring.flyway.user=" + required("SCMS_QA_DB_USER"), "--spring.flyway.password=" + required("SCMS_QA_DB_PASSWORD"), "--scms.cors.allowed-origins=http://127.0.0.1:5189", "--scms.email.provider=smtp", "--spring.mail.host=127.0.0.1", "--spring.mail.port=1", "--scms.notification-outbox-initial-delay-ms=86400000", "--scms.course-run-closure-initial-delay-ms=250", "--scms.course-run-closure-delay-ms=250", "--scms.course-session-disruption-delay-ms=250", "--scms.course-session-disruption-initial-delay-ms=250", "--sepay.bank-code=MB", "--sepay.bank-account=123456789", "--sepay.bank-account-name=SCMS GYM", "--sepay.bank-destination-sha256=7128318f4501a30f6c5648627306786fb6e7a26a15da6850a959c5b35a0a6859", "--sepay.webhook-api-key=qa-hook-only");
		var fixture = new CourseQaFixtures(context.getBean(org.springframework.jdbc.core.JdbcTemplate.class));
		var data = new LinkedHashMap<String, Object>();
		for (var entry : Map.of("manager", AccountRole.MANAGER, "reception", AccountRole.RECEPTIONIST, "coach", AccountRole.COACH, "coach2", AccountRole.COACH, "member", AccountRole.MEMBER, "member2", AccountRole.MEMBER).entrySet()) data.put(entry.getKey(), fixture.account(entry.getKey(), entry.getValue()));
		data.put("port", ((WebServerApplicationContext) context).getWebServer().getPort());
		data.put("pid", ProcessHandle.current().pid());
		// Publish readiness only after the complete JSON is visible to the parent JVM.
		Path ready = directory.resolve("ready.tmp");
		Files.writeString(ready, context.getBean(ObjectMapper.class).writeValueAsString(data));
		Files.move(ready, directory.resolve("ready.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	static String required(String key) {
		return Objects.requireNonNull(System.getenv(key), key + " required");
	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Controls {

		@Bean
		@Primary
		Clock qaClock() {
			return new Clock() {

				public ZoneId getZone() {
					return ZoneOffset.UTC;
				}

				public Clock withZone(ZoneId zone) {
					return this;
				}

				public Instant instant() {
					Path file = Path.of(required("SCMS_QA_DIRECTORY"), "clock.txt");
					try {
						return Files.exists(file) ? Instant.parse(Files.readString(file).trim()) : Instant.now();
					} catch (Exception e) {
						throw new IllegalStateException("Invalid QA clock", e);
					}
				}
			};
		}

		@Bean
		@Primary
		org.springframework.security.oauth2.jwt.JwtDecoder qaJwtDecoder(javax.crypto.SecretKey key, com.scms.backend.auth.ActiveAccountJwtValidator active, Clock clock) {
			var decoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey(key).macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
			var timestamps = new org.springframework.security.oauth2.jwt.JwtTimestampValidator();
			timestamps.setClock(clock);
			decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(timestamps, active));
			return decoder;
		}

		@Bean
		FilterRegistrationBean<jakarta.servlet.Filter> committedResponseBarrier() {
			var filter = new FilterRegistrationBean<jakarta.servlet.Filter>((request, response, chain) -> {
				var http = (jakarta.servlet.http.HttpServletRequest) request;
				if (!"after-commit".equals(http.getHeader("X-QA-Barrier"))) {
					chain.doFilter(request, response);
					return;
				}
				var cached = new ContentCachingResponseWrapper((jakarta.servlet.http.HttpServletResponse) response);
				chain.doFilter(request, cached);
				if (cached.getStatus() == 200) {
					Files.writeString(Path.of(required("SCMS_QA_DIRECTORY"), "after-commit"), "response withheld");
					try {
						new CountDownLatch(1).await(60, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				cached.copyBodyToResponse();
			});
			filter.setOrder(Integer.MIN_VALUE);
			return filter;
		}
	}
}
