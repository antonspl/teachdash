package com.teachdash.items.domain.matching;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record MatchingPayload(
		@NotEmpty @Size(min = MIN_PAIRS, max = MAX_PAIRS) List<@Valid MatchingPair> pairs) {

	public static final int MIN_PAIRS = 2;

	public static final int MAX_PAIRS = 25;
}
