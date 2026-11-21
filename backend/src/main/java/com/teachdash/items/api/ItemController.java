package com.teachdash.items.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.common.ApiException;

@RestController
class ItemController {

	// STUB: replaced by the real endpoint in pass 2 (tasks 3.2 to 3.4)
	@PostMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items")
	void create(@PathVariable UUID courseId, @PathVariable UUID lessonId, @PathVariable UUID partId) {
		throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "STUB");
	}
}
