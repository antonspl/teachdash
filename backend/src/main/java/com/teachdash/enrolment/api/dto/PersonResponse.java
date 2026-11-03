package com.teachdash.enrolment.api.dto;

import java.util.UUID;

import com.teachdash.enrolment.domain.Person;

public record PersonResponse(
		UUID id,
		String name,
		String email,
		String timeZone) {

	public static PersonResponse from(Person person) {
		return new PersonResponse(
				person.id(),
				person.name(),
				person.email(),
				person.timeZone());
	}
}
