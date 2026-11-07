package com.teachdash.accounts.api;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import com.teachdash.accounts.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Starts a session for a user: principal name = user id, authority ROLE_<role>. */
@Component
public class SessionStarter {

	private final HttpSessionSecurityContextRepository contextRepository =
			new HttpSessionSecurityContextRepository();
	private final CsrfAuthenticationStrategy csrfStrategy;

	SessionStarter(CsrfTokenRepository csrfTokenRepository) {
		this.csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
	}

	public void start(User user, HttpServletRequest request, HttpServletResponse response) {
		start(user, user.getSessionVersion(), request, response);
	}

	/** Replaces any current session with a new one that holds the given session version. */
	void start(User user, int sessionVersion, HttpServletRequest request, HttpServletResponse response) {
		var authorities = AuthorityUtils.createAuthorityList("ROLE_" + user.getRole());
		var principal = new org.springframework.security.core.userdetails.User(
				user.getId().toString(),
				"",
				authorities);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
		var strategy = SecurityContextHolder.getContextHolderStrategy();
		SecurityContext context = strategy.createEmptyContext();
		context.setAuthentication(authentication);
		HttpSession previousSession = request.getSession(false);
		if (previousSession != null) {
			previousSession.invalidate();
		}
		request.getSession(true)
				.setAttribute(SessionRevocationFilter.VERSION_ATTRIBUTE, sessionVersion);
		strategy.setContext(context);
		contextRepository.saveContext(context, request, response);
		csrfStrategy.onAuthentication(authentication, request, response);
		sendNewCsrfCookie(request);
	}

	/** The strategy replaces the token lazily; reading it makes this response set the new cookie. */
	private static void sendNewCsrfCookie(HttpServletRequest request) {
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (token != null) {
			token.getToken();
		}
	}
}
