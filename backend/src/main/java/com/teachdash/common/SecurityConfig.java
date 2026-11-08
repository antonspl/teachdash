package com.teachdash.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import com.teachdash.accounts.api.SessionRevocationFilter;
import com.teachdash.accounts.persistence.UserRepository;


@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	// Login is done by AuthController; this bean only stops Boot generating a default user and password.
	@Bean
	UserDetailsService noUserDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("login uses /api/auth/login");
		};
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
		return repository;
	}

	@Bean
	SecurityFilterChain filterChain(
			HttpSecurity http,
			CsrfTokenRepository csrfRepository,
			UserRepository users) throws Exception {
		return http
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/teacher/**").hasRole("TEACHER")
						.requestMatchers("/api/student/**").hasRole("STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/password-links",
								"/api/join-links/preview", "/api/join-links/join").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf
						.spa()
						.csrfTokenRepository(csrfRepository))
				.addFilterAfter(new SessionRevocationFilter(users), SecurityContextHolderFilter.class)
				.requestCache(cache -> cache.requestCache(new NullRequestCache()))
				.logout(AbstractHttpConfigurer::disable)
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) ->
								ProblemJson.write(response, 401, "Unauthorized", "UNAUTHENTICATED"))
						.accessDeniedHandler((request, response, exception) ->
								ProblemJson.write(response, 403, "Forbidden", deniedCode(exception))))
				.build();
	}

	private static String deniedCode(Exception exception) {
		return exception instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN";
	}
}
