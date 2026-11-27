package com.teachdash.items.domain.gapfill;

import java.util.List;

import jakarta.validation.constraints.Size;

public record GapFillSegment(
		String type,
		@Size(max = 500) String text,
		@Size(max = 5) List<String> answers) {
}
