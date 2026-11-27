package com.teachdash.items.domain.gapfill;

import java.util.List;

// STUB: no validation yet (task 3.1)
public record GapFillSegment(
		String type,
		String text,
		List<String> answers) {
}
