package com.teachdash.items.domain.matching;

import java.util.List;

public record MatchingPayload(
		List<MatchingPair> pairs) {
}
