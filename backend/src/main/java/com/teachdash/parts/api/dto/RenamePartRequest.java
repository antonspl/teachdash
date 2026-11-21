package com.teachdash.parts.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;

public record RenamePartRequest(@Title String title) {

	public RenamePartRequest {
		title = Normalize.name(title);
	}
}
