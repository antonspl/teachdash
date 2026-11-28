package com.teachdash.items.domain.wordbox;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record WordBoxPayload(
		@NotEmpty @Size(max = MAX_SEGMENTS) List<@Valid WordBoxSegment> segments,
		@Size(max = MAX_EXTRA_WORDS) List<String> extraWords) {

	public static final int MAX_SEGMENTS = 51;

	public static final int MAX_EXTRA_WORDS = 10;
}
