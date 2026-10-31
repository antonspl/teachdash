package com.teachdash.accounts.api;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.LoginRequest;
import com.teachdash.accounts.api.dto.UserResponse;
import com.teachdash.accounts.domain.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Logout is handled by the security filter chain (POST /api/auth/logout). */
@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AccountService accounts;
	private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

	AuthController(AccountService accounts) {
		this.accounts = accounts;
	}

	@PostMapping("/login")
	UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		com.teachdash.accounts.domain.User user = accounts.authenticate(request.email(), request.password());
		var authorities = AuthorityUtils.createAuthorityList("ROLE_" + user.getRole());
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				new User(user.getId().toString(), "", authorities), null, authorities);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		if (httpRequest.getSession(false) != null) {
			httpRequest.changeSessionId();
		}
		SecurityContextHolder.setContext(context);
		contextRepository.saveContext(context, httpRequest, httpResponse);
		return UserResponse.of(user);
	}
}
