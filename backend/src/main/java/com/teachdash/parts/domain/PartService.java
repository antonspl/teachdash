package com.teachdash.parts.domain;

import java.math.BigInteger;
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

	@Transactional
	public Part rename(UUID teacherId, UUID courseId, UUID lessonId, UUID partId, String title) {
		lockForWrite(teacherId, courseId, lessonId);
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		if (parts.updateTitle(partId, lessonId, teacherId, title, now) == 0) {
			throw ApiException.notFound();
		}
		return parts.findInLesson(partId, lessonId, teacherId).orElseThrow(ApiException::notFound);
	}

	@Transactional
	public Part move(UUID teacherId, UUID courseId, UUID lessonId, UUID partId, BigInteger requested) {
		lockForWrite(teacherId, courseId, lessonId);
		int old = parts.findInLesson(partId, lessonId, teacherId)
				.orElseThrow(ApiException::notFound)
				.position();
		int count = parts.countInLesson(lessonId);
		int target = requested.max(BigInteger.ONE).min(BigInteger.valueOf(count)).intValueExact();
		if (target > old) {
			parts.shiftRange(lessonId, old + 1, target, -1);
		} else if (target < old) {
			parts.shiftRange(lessonId, target, old - 1, 1);
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		parts.setPosition(partId, lessonId, teacherId, target, now);
		return parts.findInLesson(partId, lessonId, teacherId).orElseThrow(ApiException::notFound);
	}

	@Transactional
	public void delete(UUID teacherId, UUID courseId, UUID lessonId, UUID partId) {
		lockForWrite(teacherId, courseId, lessonId);
		int old = parts.deleteReturningPosition(partId, lessonId, teacherId).orElseThrow(ApiException::notFound);
		parts.closeGap(lessonId, old);
	}

	private void lockForWrite(UUID teacherId, UUID courseId, UUID lessonId) {
		// The user row is locked first, as in CourseService.rename, to avoid a deadlock.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!parts.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!parts.lockLesson(lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
	}

	@Transactional(readOnly = true)
	public List<Part> list(UUID teacherId, UUID courseId, UUID lessonId) {
		if (!parts.lessonExists(lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
		return parts.findByLesson(lessonId, teacherId);
	}
}
