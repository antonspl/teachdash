package com.teachdash.accounts.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
		this.listedLanguages = listedLanguages;
		this.sessionStarter = sessionStarter;
	}

	@GetMapping
	UserResponse me(Principal principal) {
		return UserResponse.of(accounts.get(UUID.fromString(principal.getName())));
	}

	@PatchMapping
	UserResponse updateProfile(
			@Valid @RequestBody UpdateProfileRequest request,
			BindingResult bindingResult,
			Principal principal) throws MethodArgumentNotValidException {
		UUID userId = UUID.fromString(principal.getName());
		String nativeLanguage = null;
		if (accounts.get(userId).getRole() == Role.TEACHER) {
			nativeLanguage = checkNativeLanguage(request.nativeLanguage(), bindingResult);
		}
		if (bindingResult.hasErrors()) {
			throw new MethodArgumentNotValidException(currentParameter(), bindingResult);
		}
		return UserResponse.of(accounts.updateProfile(userId, request.name(), request.timeZone(), nativeLanguage));
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
			result.addError(new FieldError("updateProfileRequest", "nativeLanguage", null, false,
					new String[] { problem }, null, null));
			return null;
		}
		return text;
	}

	private static MethodParameter currentParameter() {
		for (var method : MeController.class.getDeclaredMethods()) {
			if (method.getName().equals("updateProfile")) {
				return new MethodParameter(method, 0);
			}
		}
		throw new IllegalStateException("updateProfile not found");
	}

	@PostMapping("/password")
	ResponseEntity<Void> changePassword(
			@Valid @RequestBody ChangePasswordRequest request,
			Principal principal,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		var changed = accounts.changePassword(
				UUID.fromString(principal.getName()),
				request.currentPassword(),
				request.newPassword(),
				SessionRevocationFilter.versionOf(httpRequest.getSession()));
		sessionStarter.start(changed.user(), changed.sessionVersion(), httpRequest, httpResponse);
		return ResponseEntity.noContent().build();
	}
}
