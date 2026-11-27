package com.teachdash.items.domain.reorder;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ReorderPayload(
		String mode,
		@NotEmpty @Size(max = MAX_ENTRIES) List<@Valid ReorderEntry> entries) {

	public static final int MAX_ENTRIES = 25;
}
