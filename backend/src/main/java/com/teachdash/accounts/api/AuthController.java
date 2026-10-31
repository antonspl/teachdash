package com.teachdash.accounts.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.LoginRequest;
import com.teachdash.accounts.api.dto.UserResponse;

// STUB: all endpoints return 501 until pass 2
@RestController
@RequestMapping("/api/auth")
class AuthController {

	@PostMapping("/login")
	ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout() {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}
}
