package com.teachdash.enrolment.api.dto;

import java.util.UUID;

public record RegisterStudentResponse(
		UUID id,
		String setPasswordLink) {
}
