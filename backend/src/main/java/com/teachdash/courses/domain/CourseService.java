package com.teachdash.courses.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.courses.persistence.CourseRepository;

@Service
public class CourseService {

	private static final String TITLE_INDEX = "courses_teacher_title_uq";

	private final UserRepository users;

	private final CourseRepository courses;

	private final Clock clock;

	private final int maxPerTeacher;

	CourseService(
			UserRepository users,
			CourseRepository courses,
			Clock clock,
			@Value("${teachdash.courses.max-per-teacher}") int maxPerTeacher) {
		this.users = users;
		this.courses = courses;
		this.clock = clock;
		this.maxPerTeacher = maxPerTeacher;
	}

	@Transactional
	public Course create(
			UUID teacherId,
			String title,
			String instructionLanguage,
			String targetLanguage) {
		// The row lock serialises the limit check, so parallel requests cannot pass it together.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (courses.countByTeacher(teacherId) >= maxPerTeacher) {
			throw new ApiException(HttpStatus.CONFLICT, "COURSE_LIMIT_REACHED");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		Course course = new Course(
				UUID.randomUUID(),
				teacherId,
				title,
				instructionLanguage,
				targetLanguage,
				now,
				now);
		try {
			courses.insert(course);
		} catch (DuplicateKeyException e) {
			if (!violatesTitleIndex(e)) {
				throw e;
			}
			throw ApiException.field(HttpStatus.CONFLICT, "TITLE_TAKEN", "title");
		}
		return course;
	}

	private static boolean violatesTitleIndex(DuplicateKeyException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof PSQLException psql
					&& psql.getServerErrorMessage() != null
					&& TITLE_INDEX.equals(psql.getServerErrorMessage().getConstraint())) {
				return true;
			}
		}
		return false;
	}
}
