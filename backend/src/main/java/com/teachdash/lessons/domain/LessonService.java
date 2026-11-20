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

import com.teachdash.accounts.domain.User;
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
		requireActiveTeacher(teacherId);
		// The course row lock serialises the limit check and the position; #107's move uses the same lock.
		if (!lessons.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (lessons.countInCourse(courseId) >= maxPerCourse) {
			throw new ApiException(HttpStatus.CONFLICT, "LESSON_LIMIT_REACHED");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		UUID id = UUID.randomUUID();
		lessons.insertLesson(new Lesson(id, teacherId, title, 0, now, now));
		int position = lessons.insertAtEnd(id, courseId);
		return new Lesson(id, teacherId, title, position, now, now);
	}

	@Transactional(readOnly = true)
	public List<Lesson> list(UUID teacherId, UUID courseId) {
		if (!lessons.courseExists(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		return lessons.findByCourse(courseId, teacherId);
	}

	private void requireActiveTeacher(UUID teacherId) {
		users.findById(teacherId)
				.filter(User::isActive)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
	}
}
