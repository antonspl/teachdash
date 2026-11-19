package com.teachdash.courses.api.dto;

import com.teachdash.common.validation.CourseTitle;
import com.teachdash.common.validation.Normalize;

public record RenameCourseRequest(@CourseTitle String title) {

	public RenameCourseRequest {
		title = Normalize.name(title);
	}
}
