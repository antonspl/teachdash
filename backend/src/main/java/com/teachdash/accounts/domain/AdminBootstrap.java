package com.teachdash.accounts.domain;

import java.time.Clock;

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

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final String email;
	private final String password;
	private final String name;

	AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder, Clock clock,
			@Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String password,
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
		if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || !StringUtils.hasText(name)
				|| users.existsByRole(Role.ADMIN)) {
			return;
		}
		users.save(User.create(email.strip(), name.strip(), Role.ADMIN, passwordEncoder.encode(password), "UTC",
				clock.instant()));
	}
}
