package com.teachdash.accounts.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.LoginRequest;
import com.teachdash.accounts.api.dto.UserResponse;
import com.teachdash.accounts.domain.AccountService;
import com.teachdash.accounts.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Logout is handled by the security filter chain (POST /api/auth/logout). */
@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AccountService accounts;
	private final SessionStarter sessionStarter;

	AuthController(AccountService accounts, SessionStarter sessionStarter) {
		this.accounts = accounts;
		this.sessionStarter = sessionStarter;
	}

	@PostMapping("/login")
	UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		User user = accounts.authenticate(request.email(), request.password());
		sessionStarter.start(user, httpRequest, httpResponse);
		return UserResponse.of(user);
	}
}
