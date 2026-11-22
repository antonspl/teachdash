package com.teachdash.items.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.common.ApiException;

@RestController
class ItemController {

	// STUB: temporary, replaced by the real endpoint in task 3.5.
	@PostMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items")
	void create() {
		throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "STUB");
	}
}
