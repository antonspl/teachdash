package com.teachdash.accounts.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
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
import com.teachdash.common.ApiException;
import com.teachdash.languages.api.validation.ListedLanguages;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/me")
class MeController {

	private final AccountService accounts;
	private final SessionStarter sessionStarter;
	private final ListedLanguages listedLanguages;

	MeController(AccountService accounts, SessionStarter sessionStarter, ListedLanguages listedLanguages) {
		this.accounts = accounts;
		this.sessionStarter = sessionStarter;
		this.listedLanguages = listedLanguages;
	}

	@GetMapping
	UserResponse me(Authentication authentication) {
		return UserResponse.of(accounts.get(UUID.fromString(authentication.getName())));
	}

	@PatchMapping
	UserResponse updateProfile(
			@Valid @RequestBody UpdateProfileRequest request,
			BindingResult bindingResult,
			Authentication authentication) {
		UUID userId = UUID.fromString(authentication.getName());
		String nativeLanguage = null;
		if (hasRole(authentication, Role.TEACHER)) {
			nativeLanguage = checkNativeLanguage(request.nativeLanguage(), bindingResult);
		}
		if (bindingResult.hasErrors()) {
			throw ApiException.validation(bindingResult);
		}
		return UserResponse.of(accounts.updateProfile(userId, request.name(), request.timeZone(), nativeLanguage));
	}

	private static boolean hasRole(Authentication authentication, Role role) {
		String authority = "ROLE_" + role.name();
		return authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
	}

	/** Returns the stripped language, or null when absent; adds a field error when it is not valid. */
	private String checkNativeLanguage(JsonNode node, BindingResult result) {
		if (node == null) {
			return null;
		}
		String problem = null;
		String text = null;
		if (node.isNull()) {
			problem = "REQUIRED";
		}
		else if (!node.isString()) {
			problem = "INVALID_TYPE";
		}
		else {
			text = node.asString().strip();
			if (text.isEmpty()) {
				problem = "REQUIRED";
			}
			else if (!listedLanguages.isListed(text)) {
				problem = "INVALID_FORMAT";
			}
		}
		if (problem != null) {
			result.rejectValue("nativeLanguage", problem);
			return null;
		}
		return text;
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
