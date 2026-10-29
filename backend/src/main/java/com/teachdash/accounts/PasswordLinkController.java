package com.teachdash.accounts;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** STUB (change accounts, task 1.3): every endpoint returns 501 until pass 2. */
@RestController
@RequestMapping("/api/password-links")
class PasswordLinkController {

	@PostMapping("/{token}")
	ResponseEntity<UserResponse> usePasswordLink(@PathVariable String token, @RequestBody SetPasswordRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}
}
