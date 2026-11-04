package com.teachdash.enrolment.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.api.dto.TeacherName;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class EnrolmentQueryService {

	private final UserRepository users;
	private final TeacherStudentRepository assignments;

	EnrolmentQueryService(UserRepository users, TeacherStudentRepository assignments) {
		this.users = users;
		this.assignments = assignments;
	}

	public List<UserSummary> studentsOfTeacher(UUID teacherId) {
		return assignments.findStudents(teacherId);
	}

	public UserSummary studentOfTeacher(UUID teacherId, UUID studentId) {
		return assignments.findStudent(teacherId, studentId)
				.orElseThrow(ApiException::notFound);
	}

	public List<UserSummary> studentsOfTeacherForAdmin(UUID teacherId) {
		requireRole(teacherId, Role.TEACHER);
		return assignments.findStudents(teacherId);
	}

	public List<UserSummary> teachersOfStudent(UUID studentId) {
		requireRole(studentId, Role.STUDENT);
		return assignments.findTeachers(studentId);
	}

	public List<TeacherName> activeTeacherNamesForStudent(UUID studentId) {
		return assignments.findActiveTeacherNames(studentId);
	}

	public UserSummary studentByEmail(String email) {
		return assignments.findStudentByEmail(email)
				.orElseThrow(ApiException::notFound);
	}

	private void requireRole(UUID id, Role role) {
		if (!users.existsByIdAndRole(id, role)) {
			throw ApiException.notFound();
		}
	}
}
