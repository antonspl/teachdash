package com.teachdash.enrolment.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.enrolment.api.dto.JoinLinkResponse;
import com.teachdash.enrolment.domain.TeacherJoinLinkService;

@RestController
class TeacherJoinLinkController {

	private final TeacherJoinLinkService service;

	TeacherJoinLinkController(TeacherJoinLinkService service) {
		this.service = service;
	}

	@GetMapping("/api/teacher/join-link")
	JoinLinkResponse view(Principal principal) {
		return service.view(teacherId(principal))
				.map(JoinLinkResponse::from)
				.orElse(new JoinLinkResponse(null, null));
	}

	@PostMapping("/api/teacher/join-link")
	JoinLinkResponse generate(Principal principal) {
		return JoinLinkResponse.from(service.generate(teacherId(principal)));
	}

	@DeleteMapping("/api/teacher/join-link")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void disable(Principal principal) {
		service.disable(teacherId(principal));
	}

	private static UUID teacherId(Principal principal) {
		return UUID.fromString(principal.getName());
	}
}
