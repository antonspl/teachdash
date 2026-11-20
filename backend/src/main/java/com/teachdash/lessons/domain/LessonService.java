package com.teachdash.lessons.domain;

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
import com.teachdash.lessons.persistence.LessonRepository;

@Service
public class LessonService {

	private final UserRepository users;

	private final LessonRepository lessons;

	private final Clock clock;

	private final int maxPerCourse;

	LessonService(
			UserRepository users,
			LessonRepository lessons,
			Clock clock,
			@Value("${teachdash.lessons.max-per-course}") int maxPerCourse) {
		this.users = users;
		this.lessons = lessons;
		this.clock = clock;
		this.maxPerCourse = maxPerCourse;
	}

	@Transactional
	public Lesson create(UUID teacherId, UUID courseId, String title) {
		// User row first so the order matches course writes (users, then course); the course lock serialises the limit and position.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!lessons.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (lessons.countInCourse(courseId) >= maxPerCourse) {
			throw new ApiException(HttpStatus.CONFLICT, "LESSON_LIMIT_REACHED");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		UUID id = UUID.randomUUID();
		int position = lessons.insertAtEnd(id, teacherId, courseId, title, now);
		return new Lesson(id, teacherId, title, position, now, now);
	}

	@Transactional
	public Lesson rename(UUID teacherId, UUID courseId, UUID lessonId, String title) {
		// The user row is locked before the course row, as in CourseService.rename, to avoid a deadlock.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!lessons.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		if (lessons.updateTitle(lessonId, courseId, teacherId, title, now) == 0) {
			throw ApiException.notFound();
		}
		return lessons.findByIdInCourse(lessonId, courseId, teacherId).orElseThrow(ApiException::notFound);
	}

	@Transactional(readOnly = true)
	public List<Lesson> list(UUID teacherId, UUID courseId) {
		if (!lessons.courseExists(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		return lessons.findByCourse(courseId, teacherId);
	}
}
