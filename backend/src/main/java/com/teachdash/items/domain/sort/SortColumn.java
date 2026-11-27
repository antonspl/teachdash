package com.teachdash.items.domain.sort;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SortColumn(
		String title,
		@NotEmpty @Size(max = MAX_WORDS) List<String> words) {

	public static final int MAX_WORDS = 25;
}
