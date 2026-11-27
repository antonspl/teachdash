package com.teachdash.items.domain.sort;

import java.util.List;

public record SortPayload(
		List<SortColumn> columns) {
}
