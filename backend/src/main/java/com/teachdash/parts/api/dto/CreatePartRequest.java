package com.teachdash.parts.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;

public record CreatePartRequest(@Title String title) {

	public CreatePartRequest {
		title = Normalize.name(title);
	}
}
