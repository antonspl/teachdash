package com.teachdash.items.domain.wordbox;

import java.util.List;

// STUB: no validation until pass 2 (tasks.md 3.1)
public record WordBoxPayload(
		List<WordBoxSegment> segments,
		List<String> extraWords) {
}
