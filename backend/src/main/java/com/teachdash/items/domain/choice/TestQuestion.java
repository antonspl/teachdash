package com.teachdash.items.domain.choice;

import java.util.List;

// STUB: pass 2 adds the Bean Validation annotations
public record TestQuestion(
		String text,
		List<TestOption> options) {
}
