package com.teachdash.items.domain.dropdown;

import java.util.List;

// STUB: bare record without validation, rewritten in task 3.1
public record DropdownSegment(
		String type,
		String text,
		List<String> options,
		Integer correct) {
}
