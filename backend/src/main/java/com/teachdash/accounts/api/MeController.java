package com.teachdash.accounts.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.ChangePasswordRequest;
import com.teachdash.accounts.api.dto.UpdateProfileRequest;
import com.teachdash.accounts.api.dto.UserResponse;
import com.teachdash.accounts.domain.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
class MeController {

	private final AccountService accounts;

	MeController(AccountService accounts) {
		this.accounts = accounts;
	}

	@GetMapping
	UserResponse me(Principal principal) {
		return UserResponse.of(accounts.get(UUID.fromString(principal.getName())));
	}

	@PatchMapping
	UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, Principal principal) {
		UUID userId = UUID.fromString(principal.getName());
		return UserResponse.of(accounts.updateProfile(userId, request.name(), request.timeZone()));
	}

	@PostMapping("/password")
	ResponseEntity<Void> changePassword(
			@Valid @RequestBody ChangePasswordRequest request,
			Principal principal,
			HttpServletRequest httpRequest) {
		int sessionVersion = accounts.changePassword(
				UUID.fromString(principal.getName()),
				request.currentPassword(),
				request.newPassword(),
				SessionRevocationFilter.versionOf(httpRequest.getSession()));
		replaceSession(httpRequest, sessionVersion);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Swaps the session for a new one with the same login. A parallel request that still holds
	 * the old session cannot write the old id back, because that session is gone.
	 */
	private static void replaceSession(HttpServletRequest request, int sessionVersion) {
		HttpSession oldSession = request.getSession();
		Object securityContext = oldSession.getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		oldSession.invalidate();
		HttpSession newSession = request.getSession(true);
		newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
		newSession.setAttribute(SessionRevocationFilter.VERSION_ATTRIBUTE, sessionVersion);
	}
}
