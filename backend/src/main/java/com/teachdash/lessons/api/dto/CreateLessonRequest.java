package com.teachdash.lessons.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;

public record CreateLessonRequest(@Title String title) {

	public CreateLessonRequest {
		title = Normalize.name(title);
	}
}
