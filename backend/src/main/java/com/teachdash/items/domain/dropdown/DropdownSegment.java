package com.teachdash.items.domain.dropdown;

import java.util.List;

import jakarta.validation.constraints.Size;

public record DropdownSegment(
		String type,
		@Size(max = DropdownItemType.MAX_TEXT_LENGTH) String text,
		@Size(min = DropdownItemType.MIN_OPTIONS, max = DropdownItemType.MAX_OPTIONS) List<String> options,
		Integer correct) {
}
