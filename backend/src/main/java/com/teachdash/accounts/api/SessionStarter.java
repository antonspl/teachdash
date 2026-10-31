package com.teachdash.accounts.api;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import com.teachdash.accounts.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Starts a session for a user: principal name = user id, authority ROLE_<role> (design D4). */
@Component
class SessionStarter {

	private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
	private final CsrfAuthenticationStrategy csrfStrategy;

	SessionStarter(CsrfTokenRepository csrfTokenRepository) {
		this.csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
	}

	void start(User user, HttpServletRequest request, HttpServletResponse response) {
		var authorities = AuthorityUtils.createAuthorityList("ROLE_" + user.getRole());
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				new org.springframework.security.core.userdetails.User(user.getId().toString(), "", authorities), null,
				authorities);
		var strategy = SecurityContextHolder.getContextHolderStrategy();
		SecurityContext context = strategy.createEmptyContext();
		context.setAuthentication(authentication);
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		strategy.setContext(context);
		contextRepository.saveContext(context, request, response);
		csrfStrategy.onAuthentication(authentication, request, response);
	}
}
