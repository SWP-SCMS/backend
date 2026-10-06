package com.scms.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.auth.AuthProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {

	private final List<String> allowedOrigins;

	SecurityConfiguration(@Value("${scms.cors.allowed-origins}") String configuredAllowedOrigins) {
		this.allowedOrigins = Arrays.stream(configuredAllowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();
		if (this.allowedOrigins.isEmpty()) {
			throw new IllegalArgumentException("At least one CORS origin must be configured");
		}
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(httpBasic -> httpBasic.disable())
			.formLogin(formLogin -> formLogin.disable())
			.logout(logout -> logout.disable())
			.requestCache(requestCache -> requestCache.disable())
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout", "/auth/register")
					.permitAll()
				.requestMatchers("/docs", "/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/membership-offers", "/membership-offers/*")
					.hasAnyRole("MEMBER", "RECEPTIONIST")
				.requestMatchers(HttpMethod.POST, "/members/me/membership-orders").hasRole("MEMBER")
				.requestMatchers(HttpMethod.GET, "/members/me/membership-orders/pending").hasRole("MEMBER")
				.requestMatchers(HttpMethod.POST, "/reception/members/*/membership-orders")
					.hasRole("RECEPTIONIST")
				.requestMatchers(HttpMethod.GET, "/reception/members/*/membership-orders/pending")
					.hasRole("RECEPTIONIST")
				.requestMatchers(HttpMethod.POST, "/reception/members/*/cash-payments")
					.hasRole("RECEPTIONIST")
				.requestMatchers(HttpMethod.POST, "/reception/members").hasRole("RECEPTIONIST")
				.requestMatchers(HttpMethod.GET, "/reception/members/*/receipts")
					.hasAnyRole("RECEPTIONIST", "MANAGER")
				.requestMatchers("/reception/members/**").hasRole("RECEPTIONIST")
				.requestMatchers("/manager/staff-accounts/**").hasRole("MANAGER")
				.requestMatchers("/manager/disciplines", "/manager/disciplines/**").hasRole("MANAGER")
				.requestMatchers("/manager/classes", "/manager/classes/**").hasRole("MANAGER")
				.requestMatchers("/manager/rooms", "/manager/rooms/**").hasRole("MANAGER")
				.requestMatchers("/manager/recurring-schedules/**").hasRole("MANAGER")
				.requestMatchers("/manager/class-sessions", "/manager/class-sessions/**").hasRole("MANAGER")
				.requestMatchers(HttpMethod.GET, "/members/class-sessions").hasRole("MEMBER")
				.requestMatchers("/manager/membership-offers/**").hasRole("MANAGER")
				.requestMatchers("/reception/membership-orders/**").hasRole("RECEPTIONIST")
				.requestMatchers(HttpMethod.POST, "/members/me/membership-orders/*/payments/sepay",
					"/members/me/membership-orders/*/bank-transfer").hasRole("MEMBER")
				.requestMatchers(HttpMethod.GET, "/members/me/receipts", "/members/me/memberships")
					.hasRole("MEMBER")
				.requestMatchers(HttpMethod.POST, "/payments/sepay/webhook").permitAll()
				.requestMatchers(HttpMethod.GET, "/payments/*/result", "/receipts/*")
					.hasAnyRole("MEMBER", "RECEPTIONIST", "MANAGER")
				.requestMatchers(HttpMethod.PATCH, "/membership-orders/*/cancel")
					.hasAnyRole("RECEPTIONIST", "MANAGER")
				.requestMatchers("/payments/**").hasAnyRole("MANAGER", "RECEPTIONIST")
				.requestMatchers("/manager/reports/**").hasRole("MANAGER")
				.requestMatchers("/manager/members/**").hasRole("MANAGER")
				.requestMatchers("/members/me/profile").hasRole("MEMBER")
				.anyRequest().authenticated())
			.oauth2ResourceServer(resourceServer -> resourceServer
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecretKey jwtSecretKey(AuthProperties properties) {
		return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
			.algorithm(MacAlgorithm.HS256)
			.build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey, ActiveAccountJwtValidator activeAccountJwtValidator) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
			activeAccountJwtValidator);
		decoder.setJwtValidator(validator);
		return decoder;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowCredentials(true);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("role");
		authoritiesConverter.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}
}
