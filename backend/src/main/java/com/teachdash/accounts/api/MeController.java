package com.teachdash.accounts.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
		return UserResponse.of(accounts.updateProfile(UUID.fromString(principal.getName()), request.name(),
				request.timeZone()));
	}

	@PostMapping("/password")
	ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Principal principal) {
		accounts.changePassword(UUID.fromString(principal.getName()), request.currentPassword(),
				request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
