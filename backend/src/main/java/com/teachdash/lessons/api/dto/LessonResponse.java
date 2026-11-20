package com.teachdash.lessons.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.teachdash.lessons.domain.Lesson;

public record LessonResponse(
		UUID id,
		String title,
		int position,
		Instant createdAt,
		Instant updatedAt) {

	public static LessonResponse from(Lesson lesson) {
		return new LessonResponse(
				lesson.id(),
				lesson.title(),
				lesson.position(),
				lesson.createdAt(),
				lesson.updatedAt());
	}
}
