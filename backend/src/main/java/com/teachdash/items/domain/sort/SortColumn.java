package com.teachdash.items.domain.sort;

import java.util.List;

public record SortColumn(
		String title,
		List<String> words) {
}
