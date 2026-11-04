package com.teachdash.enrolment.domain;

import java.util.UUID;

public record UserSummary(
		UUID id,
		String name,
		String email,
		String timeZone,
		boolean active) {
}
