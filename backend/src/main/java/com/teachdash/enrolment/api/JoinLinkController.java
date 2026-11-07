package com.teachdash.enrolment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.SessionStarter;
import com.teachdash.accounts.domain.User;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.api.dto.JoinLinkJoinRequest;
import com.teachdash.enrolment.api.dto.JoinLinkTokenRequest;
import com.teachdash.enrolment.api.dto.JoinPreviewResponse;
import com.teachdash.enrolment.api.dto.JoinVisitor;
import com.teachdash.enrolment.api.dto.JoinedStudentResponse;
import com.teachdash.enrolment.api.dto.JoinedVisitorResponse;
import com.teachdash.enrolment.domain.JoinLinkService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;

/** Check order: role 403, validation 400, link 410, email 409. */
@RestController
class JoinLinkController {

	private final JoinLinkService service;
	private final SessionStarter sessionStarter;
	private final Validator validator;

	JoinLinkController(JoinLinkService service, SessionStarter sessionStarter, Validator validator) {
		this.service = service;
		this.sessionStarter = sessionStarter;
		this.validator = validator;
	}

	@PostMapping("/api/join-links/preview")
	JoinPreviewResponse preview(@RequestBody JoinLinkTokenRequest request, Authentication authentication) {
		rejectTeacherAndAdmin(authentication);
		validate(request);
		return new JoinPreviewResponse(service.previewTeacherName(request.token()));
	}

	@PostMapping("/api/join-links/join")
	ResponseEntity<Object> join(
			@RequestBody JoinLinkJoinRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		rejectTeacherAndAdmin(authentication);
		validate(request);
		if (hasRole(authentication, "ROLE_STUDENT")) {
			UUID studentId = UUID.fromString(authentication.getName());
			return ResponseEntity.ok(new JoinedStudentResponse(service.joinAsStudent(request.token(), studentId)));
		}
		JoinVisitor visitor = JoinVisitor.of(request);
		validate(visitor);
		User student = service.joinAsVisitor(
				request.token(),
				visitor.name(),
				visitor.email(),
				visitor.timeZone(),
				visitor.password());
		sessionStarter.start(student, httpRequest, httpResponse);
		return ResponseEntity.status(HttpStatus.CREATED).body(new JoinedVisitorResponse(student.getId()));
	}

	private void validate(Object request) {
		if (!validator.validate(request).isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		}
	}

	private static void rejectTeacherAndAdmin(Authentication authentication) {
		if (hasRole(authentication, "ROLE_TEACHER") || hasRole(authentication, "ROLE_ADMIN")) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN");
		}
	}

	private static boolean hasRole(Authentication authentication, String role) {
		return authentication != null
				&& !(authentication instanceof AnonymousAuthenticationToken)
				&& authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
	}
}
