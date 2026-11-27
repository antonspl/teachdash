package com.teachdash.items.domain.reorder;

import java.util.List;

// STUB: no validation yet (pass 2, task 3.1)
public record ReorderEntry(
		List<String> units,
		String hint) {
}
