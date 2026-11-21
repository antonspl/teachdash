package com.teachdash.courseassignment.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.courseassignment.persistence.CourseAssignmentRepository;
import com.teachdash.courses.persistence.CourseRepository;

@Service
public class CourseAssignmentService {

	private final UserRepository users;

	private final CourseRepository courses;

	private final CourseAssignmentRepository assignments;

	private final Clock clock;

	CourseAssignmentService(
			UserRepository users,
			CourseRepository courses,
			CourseAssignmentRepository assignments,
			Clock clock) {
		this.users = users;
		this.courses = courses;
		this.assignments = assignments;
		this.clock = clock;
	}

	@Transactional
	public void assign(UUID teacherId, UUID courseId, UUID studentId) {
		lockTeacherCourseAndStudent(teacherId, courseId, studentId);
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		assignments.insertIfAbsent(courseId, studentId, teacherId, now);
	}

	@Transactional
	public void unassign(UUID teacherId, UUID courseId, UUID studentId) {
		lockTeacherCourseAndStudent(teacherId, courseId, studentId);
		if (!assignments.delete(courseId, studentId, teacherId)) {
			throw ApiException.notFound();
		}
	}

	@Transactional(readOnly = true)
	public List<CourseStudent> list(UUID teacherId, UUID courseId) {
		courses.findByIdAndTeacher(courseId, teacherId).orElseThrow(ApiException::notFound);
		return assignments.findStudentsOfCourse(courseId, teacherId);
	}

	private void lockTeacherCourseAndStudent(UUID teacherId, UUID courseId, UUID studentId) {
		// Teacher row first, as in LessonService and StudentRemovalService, so lock orders cannot form a cycle.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!courses.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!assignments.studentOfTeacherExists(teacherId, studentId)) {
			throw ApiException.notFound();
		}
	}
}
