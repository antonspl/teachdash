package com.teachdash.accounts.domain;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.teachdash.accounts.persistence.UserRepository;

/** Creates the first admin from ADMIN_* variables when no admin exists (R1). */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final String email;
	private final String password;
	private final String name;

	AdminBootstrap(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			Clock clock,
			@Value("${ADMIN_EMAIL:}") String email,
			@Value("${ADMIN_PASSWORD:}") String password,
			@Value("${ADMIN_NAME:}") String name) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.email = email;
		this.password = password;
		this.name = name;
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
