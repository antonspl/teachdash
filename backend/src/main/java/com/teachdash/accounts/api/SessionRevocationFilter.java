package com.teachdash.accounts.api;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.teachdash.accounts.domain.User;
import com.teachdash.accounts.persistence.UserRepository;

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

	private final UserRepository users;

	public SessionRevocationFilter(UserRepository users) {
		this.users = users;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		HttpSession session = request.getSession(false);
		if (authentication != null && session != null && isRevoked(authentication, session)) {
			session.invalidate();
			SecurityContextHolder.clearContext();
		}
		chain.doFilter(request, response);
	}

	private boolean isRevoked(Authentication authentication, HttpSession session) {
		UUID userId = UUID.fromString(authentication.getName());
		int sessionVersion = session.getAttribute(VERSION_ATTRIBUTE) instanceof Integer version ? version : 0;
		return users.findById(userId)
				.map(user -> isRevoked(user, sessionVersion))
				.orElse(true);
	}

	private static boolean isRevoked(User user, int sessionVersion) {
		return !user.isActive() || user.getSessionVersion() != sessionVersion;
	}
}
