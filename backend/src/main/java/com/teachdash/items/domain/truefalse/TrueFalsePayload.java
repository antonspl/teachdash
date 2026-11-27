package com.teachdash.items.domain.truefalse;

import java.util.List;

/** STUB: no constraints yet. */
public record TrueFalsePayload(
		Boolean allowNotStated,
		List<TrueFalseStatement> statements) {
}
