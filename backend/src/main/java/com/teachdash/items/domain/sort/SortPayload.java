package com.teachdash.items.domain.sort;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SortPayload(
		@NotEmpty @Size(min = MIN_COLUMNS, max = MAX_COLUMNS) List<@Valid SortColumn> columns) {

	public static final int MIN_COLUMNS = 2;

	public static final int MAX_COLUMNS = 10;
}
