package com.teachdash.accounts.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.SetPasswordRequest;
import com.teachdash.accounts.api.dto.UserResponse;

// STUB: all endpoints return 501 until pass 2
@RestController
@RequestMapping("/api/password-links")
class PasswordLinkController {

	@PostMapping
	ResponseEntity<UserResponse> usePasswordLink(@RequestBody SetPasswordRequest request) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}
}
