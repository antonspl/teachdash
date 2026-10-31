package com.teachdash.accounts.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.SetPasswordRequest;
import com.teachdash.accounts.api.dto.UserResponse;
import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.accounts.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/password-links")
class PasswordLinkController {

	private final PasswordLinkService links;
	private final SessionStarter sessionStarter;

	PasswordLinkController(PasswordLinkService links, SessionStarter sessionStarter) {
		this.links = links;
		this.sessionStarter = sessionStarter;
	}

	@PostMapping
	UserResponse usePasswordLink(@Valid @RequestBody SetPasswordRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		User user = links.use(request.token(), request.password());
		sessionStarter.start(user, httpRequest, httpResponse);
		return UserResponse.of(user);
	}
}
