package com.scms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FoundationIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private MemberProfileRepository memberProfileRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SecurityFilterChain securityFilterChain;

	@Autowired
	private CorsConfigurationSource corsConfigurationSource;

	@Autowired
	private Clock clock;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void postgresStartsFlywayAppliesAllMigrationsAndHibernateValidates() {
		assertThat(postgres.isRunning()).isTrue();
		assertThat(entityManagerFactory.isOpen()).isTrue();
		assertThat(jdbcTemplate.queryForList(
			"select version from flyway_schema_history where success order by installed_rank", String.class))
			.containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
		assertThat(jdbcTemplate.queryForObject("show timezone", String.class)).isIn("UTC", "Etc/UTC");
	}

	@Test
	@Transactional
	void accountAndMemberProfileRepositoriesUseTheMigratedSchema() {
		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Foundation Member",
			"090" + accountId.toString().substring(0, 8), accountId + "@example.test", LocalDate.of(1990, 1, 1),
			"{noop}foundation-password");

		Account savedAccount = accountRepository.saveAndFlush(account);
		MemberProfile savedProfile = memberProfileRepository.saveAndFlush(new MemberProfile(account));

		assertThat(savedAccount.getCreatedAt()).isNotNull();
		assertThat(savedAccount.getUpdatedAt()).isNotNull();
		assertThat(savedProfile.getMemberCode()).matches("MB-[0-9]+");
		assertThat(savedProfile.getCreatedAt()).isNotNull();
		assertThat(savedProfile.getUpdatedAt()).isNotNull();
		entityManager.clear();

		assertThat(accountRepository.findById(accountId))
			.get()
			.extracting(Account::getEmail)
			.isEqualTo(accountId + "@example.test");
		assertThat(memberProfileRepository.findById(accountId))
			.get()
			.extracting(MemberProfile::getMemberCode)
			.asString()
			.matches("MB-[0-9]+");
	}

	@Test
	void passwordEncoderHashesAndVerifiesPasswords() {
		String encoded = passwordEncoder.encode("correct horse battery staple");

		assertThat(encoded).isNotEqualTo("correct horse battery staple");
		assertThat(passwordEncoder.matches("correct horse battery staple", encoded)).isTrue();
		assertThat(passwordEncoder.matches("wrong password", encoded)).isFalse();
	}

	@Test
	void securityCorsAndUtcClockBeansStart() {
		assertThat(securityFilterChain).isNotNull();
		CorsConfiguration cors = corsConfigurationSource
			.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/foundation"));
		assertThat(cors).isNotNull();
		assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:5173");
		assertThat(cors.getAllowCredentials()).isTrue();
		assertThat(clock.getZone()).isEqualTo(Clock.systemUTC().getZone());
	}

	@Test
	void corsPreflightAllowsConfiguredCredentialedOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/foundation")
			.contextPath("/api/v1")
			.header(HttpHeaders.ORIGIN, "http://localhost:5173")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}
}
