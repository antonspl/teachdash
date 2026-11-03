package com.teachdash.enrolment.domain;

import java.util.UUID;

public record Person(
		UUID id,
		String name,
		String email,
		String timeZone) {
}
