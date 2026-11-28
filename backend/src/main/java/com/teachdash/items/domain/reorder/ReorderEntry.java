package com.teachdash.items.domain.reorder;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ReorderEntry(
		@NotEmpty @Size(min = MIN_UNITS, max = MAX_UNITS) List<String> units,
		String hint) {

	public static final int MIN_UNITS = 2;

	public static final int MAX_UNITS = 25;
}
