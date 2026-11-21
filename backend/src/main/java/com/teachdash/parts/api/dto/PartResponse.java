package com.teachdash.parts.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.teachdash.parts.domain.Part;

public record PartResponse(
		UUID id,
		String title,
		int position,
		Instant createdAt,
		Instant updatedAt) {

	public static PartResponse from(Part part) {
		return new PartResponse(
				part.id(),
				part.title(),
				part.position(),
				part.createdAt(),
				part.updatedAt());
	}
}
