package com.teachdash.accounts.api;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.teachdash.accounts.domain.User;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ProblemJson;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Rejects a session whose account is inactive or whose session version is older than the
 * account's (design D4). The request then continues unauthenticated and gets the normal 401.
 */
public class SessionRevocationFilter extends OncePerRequestFilter {

	public static final String VERSION_ATTRIBUTE = "SESSION_VERSION";

	private static final Logger log = LoggerFactory.getLogger(SessionRevocationFilter.class);

	private final UserRepository users;

	public SessionRevocationFilter(UserRepository users) {
		this.users = users;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		try {
			rejectRevokedSession(request);
		} catch (RuntimeException e) {
			log.error("Session check failed", e);
			ProblemJson.write(response, 500, "Internal Server Error", "INTERNAL_ERROR");
			return;
		}
		chain.doFilter(request, response);
	}

	/**
	 * An inactive or missing account ends its session. A version mismatch only rejects this
	 * request, because the session may belong to a device that just changed the password.
	 */
	private void rejectRevokedSession(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		HttpSession session = request.getSession(false);
		if (authentication == null || session == null) {
			return;
		}
		Optional<User> user = users.findById(UUID.fromString(authentication.getName()));
		if (user.isEmpty() || !user.get().isActive()) {
			session.invalidate();
			SecurityContextHolder.clearContext();
		} else if (user.get().getSessionVersion() != versionOf(session)) {
			SecurityContextHolder.clearContext();
		}
	}

	/** A session without the attribute counts as version 0. */
	public static int versionOf(HttpSession session) {
		return session.getAttribute(VERSION_ATTRIBUTE) instanceof Integer version ? version : 0;
	}
}
