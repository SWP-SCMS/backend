package com.scms.backend.qa;

import com.scms.backend.account.AccountRole;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Local/CI E2E owner: a fresh database, migrations, accounts and real HTTP backend.
 */
public final class CourseQaEnvironment {

	public static void main(String[] args) throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Path dir = Path.of(args[0]).toAbsolutePath();
		Files.createDirectories(dir);
		try (var postgres = new PostgreSQLContainer("postgres:17")) {
			postgres.start();
			try (var app = new CourseQaProcess(dir, postgres)) {
				app.start();
				var db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
				var fixture = new CourseQaFixtures(db);
				var json = new ObjectMapper();
				var data = new LinkedHashMap<String, Object>();
				data.put("backendUrl", app.base());
				data.put("backendPid", app.process.pid());
				data.put("clockMode", "SYSTEM_CLOCK");
				data.put("backendSha", System.getenv().getOrDefault("SCMS_QA_BACKEND_SHA", "UNRECORDED"));
				var people = new LinkedHashMap<String, Object>();
				for (var entry : Map.of("manager", AccountRole.MANAGER, "reception", AccountRole.RECEPTIONIST, "coach", AccountRole.COACH, "coach2", AccountRole.COACH, "member", AccountRole.MEMBER, "member2", AccountRole.MEMBER).entrySet()) {
					UUID id = fixture.account(entry.getKey(), entry.getValue());
					var person = new LinkedHashMap<String, Object>();
					person.put("accountId", id);
					person.put("email", entry.getKey() + "@course-qa.invalid");
					if (entry.getValue() == AccountRole.MEMBER)
						person.put("memberCode", db.queryForObject("select member_code from member_profiles where account_id=?", String.class, id));
					people.put(entry.getKey(), person);
				}
				UUID inactive = fixture.account("inactive", AccountRole.MEMBER);
				db.update("update accounts set status='INACTIVE' where id=?", inactive);
				data.put("accounts", people);
				var payments = new LinkedHashMap<String, Object>();
				for (String scenario : List.of("cash", "reconcile", "withdraw", "attendance", "transfer", "waitlist")) {
					var p = fixture.payment(fixture.account("member", AccountRole.MEMBER), Instant.now().plusSeconds(600), scenario.equals("reconcile"), 10);
					payments.put(scenario, p);
				}
				data.put("fixtures", payments);
				Files.writeString(dir.resolve("environment.json"), json.writeValueAsString(data));
				// The owner script writes stop; no terminal/PID discovery is used for teardown.
				CourseQaProcess.await(() -> Files.exists(dir.resolve("stop")), Duration.ofMinutes(30));
			}
		}
	}
}
