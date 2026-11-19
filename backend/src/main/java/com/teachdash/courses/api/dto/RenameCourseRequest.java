package com.teachdash.courses.api.dto;

import com.teachdash.common.validation.CourseTitle;
import com.teachdash.common.validation.Normalize;

// STUB (courses-read pass 1): minimal; pass 2 finishes it
public record RenameCourseRequest(@CourseTitle String title) {

	public RenameCourseRequest {
		title = Normalize.name(title);
	}
}
