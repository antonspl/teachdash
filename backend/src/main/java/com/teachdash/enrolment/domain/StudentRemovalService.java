package com.teachdash.enrolment.domain;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.accounts.domain.TokenPurpose;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class StudentRemovalService {

	private final TeacherStudentRepository assignments;

	private final PasswordLinkService links;

	StudentRemovalService(TeacherStudentRepository assignments, PasswordLinkService links) {
		this.assignments = assignments;
		this.links = links;
	}

	@Transactional
	public void removeStudent(UUID teacherId, UUID studentId) {
		if (!assignments.delete(teacherId, studentId)) {
			throw ApiException.notFound();
		}
		links.invalidateLinks(studentId, TokenPurpose.SET_PASSWORD);
	}
}
