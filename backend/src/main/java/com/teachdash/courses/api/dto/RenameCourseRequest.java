package com.teachdash.courses.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;

public record RenameCourseRequest(@Title String title) {

	public RenameCourseRequest {
		title = Normalize.name(title);
	}
}
