package com.teachdash.parts.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.parts.api.dto.CreatePartRequest;
import com.teachdash.parts.api.dto.MovePartRequest;
import com.teachdash.parts.api.dto.PartResponse;
import com.teachdash.parts.api.dto.RenamePartRequest;
import com.teachdash.parts.domain.PartService;

import jakarta.validation.Valid;

@RestController
class PartController {

	private final PartService parts;

	PartController(PartService parts) {
		this.parts = parts;
	}

	@PostMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts")
	@ResponseStatus(HttpStatus.CREATED)
	PartResponse create(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@Valid @RequestBody CreatePartRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return PartResponse.from(parts.create(teacherId, courseId, lessonId, request.title()));
	}

	@GetMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts")
	List<PartResponse> list(@PathVariable UUID courseId, @PathVariable UUID lessonId, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return parts.list(teacherId, courseId, lessonId).stream().map(PartResponse::from).toList();
	}

	@PatchMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}")
	PartResponse rename(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			@Valid @RequestBody RenamePartRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return PartResponse.from(parts.rename(teacherId, courseId, lessonId, partId, request.title()));
	}

	@PatchMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/position")
	PartResponse move(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			@Valid @RequestBody MovePartRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return PartResponse.from(parts.move(teacherId, courseId, lessonId, partId, request.position()));
	}

	@DeleteMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		parts.delete(teacherId, courseId, lessonId, partId);
	}
}
