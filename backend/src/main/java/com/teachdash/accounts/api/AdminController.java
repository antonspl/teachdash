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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.CreateTeacherRequest;
import com.teachdash.accounts.api.dto.CreateTeacherResponse;
import com.teachdash.accounts.api.dto.LinkResponse;
import com.teachdash.accounts.api.dto.SetPasswordLinkResponse;
import com.teachdash.accounts.api.dto.TeacherResponse;
import com.teachdash.accounts.domain.TeacherAdminService;

import jakarta.validation.Valid;

/** Admin-only; the role rule is in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
class AdminController {

	private final TeacherAdminService teachers;

	AdminController(TeacherAdminService teachers) {
		this.teachers = teachers;
	}

	@GetMapping("/teachers")
	List<TeacherResponse> listTeachers() {
		return teachers.listTeachers().stream().map(TeacherResponse::of).toList();
	}

	@PostMapping("/teachers")
	@ResponseStatus(HttpStatus.CREATED)
	CreateTeacherResponse createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
		var created = teachers.createTeacher(request.name(), request.email(), request.timeZone());
		return new CreateTeacherResponse(created.id(), created.setPasswordLink());
	}

	@PostMapping("/teachers/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deactivate(@PathVariable UUID id) {
		teachers.deactivateTeacher(id);
	}

	/** 200 with a new set-password link if the teacher never set a password, otherwise 204. */
	@PostMapping("/teachers/{id}/reactivate")
	ResponseEntity<SetPasswordLinkResponse> reactivate(@PathVariable UUID id) {
		return teachers.reactivateTeacher(id)
				.map(link -> ResponseEntity.ok(new SetPasswordLinkResponse(link)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/users/{id}/reset-link")
	@ResponseStatus(HttpStatus.CREATED)
	LinkResponse resetLink(@PathVariable UUID id) {
		return new LinkResponse(teachers.issueResetLink(id));
	}
}
