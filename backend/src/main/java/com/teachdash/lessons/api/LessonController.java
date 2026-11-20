package com.teachdash.lessons.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.lessons.api.dto.CreateLessonRequest;
import com.teachdash.lessons.api.dto.LessonResponse;
import com.teachdash.lessons.domain.LessonService;

import jakarta.validation.Valid;

@RestController
class LessonController {

	private final LessonService lessons;

	LessonController(LessonService lessons) {
		this.lessons = lessons;
	}

	@PostMapping("/api/teacher/courses/{courseId}/lessons")
	@ResponseStatus(HttpStatus.CREATED)
	LessonResponse create(
			@PathVariable UUID courseId,
			@Valid @RequestBody CreateLessonRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return LessonResponse.from(lessons.create(teacherId, courseId, request.title()));
	}

	@GetMapping("/api/teacher/courses/{courseId}/lessons")
	List<LessonResponse> list(@PathVariable UUID courseId, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return lessons.list(teacherId, courseId).stream().map(LessonResponse::from).toList();
	}

	// STUB: replaced in pass 2 (lessons-edit-delete 3.2)
	@PatchMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}")
	LessonResponse rename(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	// STUB: replaced in pass 2 (lessons-edit-delete 3.2)
	@PatchMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/position")
	LessonResponse move(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	// STUB: replaced in pass 2 (lessons-edit-delete 3.2)
	@DeleteMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}")
	void delete(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}
}
