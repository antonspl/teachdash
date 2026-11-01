package com.teachdash.accounts.domain;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.teachdash.accounts.api.dto.ChangePasswordRequest;
import com.teachdash.accounts.api.dto.CreateTeacherRequest;
import com.teachdash.accounts.persistence.UserRepository;

import jakarta.validation.Validator;

/** Creates the first admin from ADMIN_* variables when no admin exists (R1). */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final Validator validator;
	private final String email;
	private final String password;
	private final String name;

	AdminBootstrap(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			Clock clock,
			Validator validator,
			@Value("${ADMIN_EMAIL:}") String email,
			@Value("${ADMIN_PASSWORD:}") String password,
			@Value("${ADMIN_NAME:}") String name) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.validator = validator;
		this.email = email;
		this.password = password;
		this.name = name;
	}

	// STUB: resolves placeholders like @Value does; task 3.21 reads the property sources directly
	static AdminBootstrap fromEnvironment(
			ConfigurableEnvironment environment,
			UserRepository users,
			PasswordEncoder passwordEncoder,
			Clock clock,
			Validator validator) {
		return new AdminBootstrap(
				users,
				passwordEncoder,
				clock,
				validator,
				environment.resolveRequiredPlaceholders("${ADMIN_EMAIL:}"),
				environment.resolveRequiredPlaceholders("${ADMIN_PASSWORD:}"),
				environment.resolveRequiredPlaceholders("${ADMIN_NAME:}"));
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.existsByRole(Role.ADMIN)) {
			return;
		}
		List<String> missing = missingVariables();
		if (!missing.isEmpty()) {
			log.warn("No admin exists and these variables are missing, so none was created: {}", missing);
			return;
		}
		requireValidVariables();
		if (users.findByEmailIgnoreCase(email.strip()).isPresent()) {
			log.warn("No admin exists but ADMIN_EMAIL belongs to another account; admin not created");
			return;
		}
		users.save(User.create(
				email.strip(),
				name.strip(),
				Role.ADMIN,
				passwordEncoder.encode(password),
				"UTC",
				clock.instant()));
		log.info("Admin created");
	}

	/** Fails startup naming the variable, never its value; the rules are the API's own constraints. */
	private void requireValidVariables() {
		requireValid("ADMIN_EMAIL", CreateTeacherRequest.class, "email", email.strip());
		requireValid("ADMIN_NAME", CreateTeacherRequest.class, "name", name.strip());
		requireValid("ADMIN_PASSWORD", ChangePasswordRequest.class, "newPassword", password);
		if (AccountService.tooLongForBcrypt(password)) {
			throw new IllegalStateException("ADMIN_PASSWORD is invalid: at most 72 bytes are allowed");
		}
	}

	private <T> void requireValid(String variable, Class<T> constraints, String field, String value) {
		if (!validator.validateValue(constraints, field, value).isEmpty()) {
			throw new IllegalStateException(variable + " is invalid: it breaks the same rules as the API field " + field);
		}
	}

	private List<String> missingVariables() {
		List<String> missing = new ArrayList<>();
		if (!StringUtils.hasText(email)) {
			missing.add("ADMIN_EMAIL");
		}
		if (!StringUtils.hasText(password)) {
			missing.add("ADMIN_PASSWORD");
		}
		if (!StringUtils.hasText(name)) {
			missing.add("ADMIN_NAME");
		}
		return missing;
	}
}
