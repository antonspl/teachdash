package com.teachdash.accounts.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.CreateTeacherRequest;
import com.teachdash.accounts.api.dto.CreateTeacherResponse;
import com.teachdash.accounts.api.dto.LinkResponse;
import com.teachdash.accounts.api.dto.UserResponse;

/** STUB (change accounts, task 1.3): every endpoint returns 501 until pass 2. */
@RestController
@RequestMapping("/api/admin")
class AdminController {

	@GetMapping("/teachers")
	ResponseEntity<List<UserResponse>> listTeachers() {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/teachers")
	ResponseEntity<CreateTeacherResponse> createTeacher(@RequestBody CreateTeacherRequest request) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/teachers/{id}/deactivate")
	ResponseEntity<Void> deactivate(@PathVariable UUID id) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/teachers/{id}/reactivate")
	ResponseEntity<Void> reactivate(@PathVariable UUID id) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/users/{id}/reset-link")
	ResponseEntity<LinkResponse> resetLink(@PathVariable UUID id) {
		// STUB
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}
}
