package com.teachdash.lessons.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;

public record RenameLessonRequest(@Title String title) {

	public RenameLessonRequest {
		title = Normalize.name(title);
	}
}
