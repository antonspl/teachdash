package com.teachdash.items.domain.wordbox;

import jakarta.validation.constraints.Size;

public record WordBoxSegment(
		String type,
		@Size(max = MAX_TEXT_LENGTH) String text,
		String answer) {

	public static final int MAX_TEXT_LENGTH = 500;
}
