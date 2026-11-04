package com.teachdash.enrolment.domain;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class StudentRemovalService {

	private final TeacherStudentRepository assignments;

	StudentRemovalService(TeacherStudentRepository assignments) {
		this.assignments = assignments;
	}

	@Transactional
	public void removeStudent(UUID teacherId, UUID studentId) {
		if (!assignments.delete(teacherId, studentId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
		}
	}
}
