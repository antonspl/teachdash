package com.teachdash.enrolment.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.common.TokenGenerator;
import com.teachdash.enrolment.persistence.TeacherJoinLinkRepository;

@Service
public class TeacherJoinLinkService {

	private final UserRepository users;
	private final TeacherJoinLinkRepository joinLinks;
	private final Clock clock;
	private final String baseUrl;

	TeacherJoinLinkService(
			UserRepository users,
			TeacherJoinLinkRepository joinLinks,
			Clock clock,
			@Value("${APP_BASE_URL}") String baseUrl) {
		this.users = users;
		this.joinLinks = joinLinks;
		this.clock = clock;
		this.baseUrl = baseUrl.endsWith("/")
				? baseUrl.substring(0, baseUrl.length() - 1)
				: baseUrl;
	}

	public record IssuedLink(
			String link,
			Instant expiresAt) {
	}

	@Transactional(readOnly = true)
	public Optional<IssuedLink> view(UUID teacherId) {
		return joinLinks.findValid(teacherId, clock.instant())
				.map(link -> new IssuedLink(url(link.token()), link.expiresAt()));
	}

	@Transactional
	public IssuedLink generate(UUID teacherId) {
		// A parallel deactivation updates this row, so it either finishes first (401 here)
		// or waits until this commits and then deletes the new link.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		String token = TokenGenerator.generate();
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		joinLinks.upsert(teacherId, token, now);
		return new IssuedLink(url(token), now.plus(TeacherJoinLinkRepository.VALIDITY));
	}

	@Transactional
	public void disable(UUID teacherId) {
		users.findByIdForUpdate(teacherId);
		joinLinks.delete(teacherId);
	}

	private String url(String token) {
		return baseUrl + "/join#token=" + token;
	}
}
