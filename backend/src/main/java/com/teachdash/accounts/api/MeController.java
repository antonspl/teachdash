package com.teachdash.accounts.api;

import org.springframework.http.HttpStatus;
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

// STUB: all endpoints return 501 until pass 2
@RestController
@RequestMapping("/api/me")
class MeController {

	@GetMapping
	ResponseEntity<UserResponse> me() {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PatchMapping
	ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/password")
	ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}
}
