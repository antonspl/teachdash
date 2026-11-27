package com.teachdash.items.domain.dropdown;

import java.util.List;

import jakarta.validation.constraints.Size;

public record DropdownSegment(
		String type,
		@Size(max = 500) String text,
		@Size(min = 2, max = 5) List<String> options,
		Integer correct) {
}
