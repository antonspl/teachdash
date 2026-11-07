package com.teachdash.enrolment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only the token is checked here; a student session ignores the rest, a visitor's are checked as {@link JoinVisitor}. */
public record JoinLinkJoinRequest(
		@NotBlank @Size(max = 64) String token,
		String name,
		String email,
		String timeZone,
		String password) {
}
