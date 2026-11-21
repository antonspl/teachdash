package com.teachdash.parts.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.parts.persistence.PartRepository;

@Service
public class PartService {

	private final UserRepository users;

	private final PartRepository parts;

	private final Clock clock;

	private final int maxPerLesson;

	PartService(
			UserRepository users,
			PartRepository parts,
			Clock clock,
			@Value("${teachdash.parts.max-per-lesson}") int maxPerLesson) {
		this.users = users;
		this.parts = parts;
		this.clock = clock;
		this.maxPerLesson = maxPerLesson;
	}

	@Transactional
	public Part create(UUID teacherId, UUID courseId, UUID lessonId, String title) {
		// The user row is locked first, as in CourseService.rename, to avoid a deadlock.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!parts.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!parts.lockLesson(lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (parts.countInLesson(lessonId) >= maxPerLesson) {
			throw new ApiException(HttpStatus.CONFLICT, "PART_LIMIT_REACHED");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		UUID id = UUID.randomUUID();
		int position = parts.insertAtEnd(id, teacherId, lessonId, title, now);
		return new Part(id, teacherId, title, position, now, now);
	}

	@Transactional(readOnly = true)
	public List<Part> list(UUID teacherId, UUID courseId, UUID lessonId) {
		if (!parts.lessonExists(lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
		return parts.findByLesson(lessonId, teacherId);
	}
}
