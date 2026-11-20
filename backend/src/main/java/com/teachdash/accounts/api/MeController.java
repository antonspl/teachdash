package com.teachdash.accounts.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.teachdash.accounts.domain.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
class MeController {

	private final AccountService accounts;
	private final SessionStarter sessionStarter;

	MeController(AccountService accounts, SessionStarter sessionStarter) {
		this.accounts = accounts;
		this.sessionStarter = sessionStarter;
	}

	@GetMapping
	UserResponse me(Authentication authentication) {
		return UserResponse.of(accounts.get(UUID.fromString(authentication.getName())));
	}

	@PatchMapping
	UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
		String nativeLanguage = isTeacher(authentication) ? request.nativeLanguage() : null;
		return UserResponse.of(accounts.updateProfile(
				UUID.fromString(authentication.getName()), request.name(), request.timeZone(), nativeLanguage));
	}

	private static boolean isTeacher(Authentication authentication) {
		String authority = "ROLE_" + Role.TEACHER.name();
		return authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
	}

	@PostMapping("/password")
	ResponseEntity<Void> changePassword(
			@Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		var changed = accounts.changePassword(
				UUID.fromString(authentication.getName()),
				request.currentPassword(),
				request.newPassword(),
				SessionRevocationFilter.versionOf(httpRequest.getSession()));
		sessionStarter.start(changed.user(), changed.sessionVersion(), httpRequest, httpResponse);
		return ResponseEntity.noContent().build();
	}
}
