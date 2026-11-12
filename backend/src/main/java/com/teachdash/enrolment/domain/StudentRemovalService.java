package com.teachdash.enrolment.domain;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class StudentRemovalService {

	private final UserRepository users;

	private final TeacherStudentRepository assignments;

	private final PasswordLinkService links;

	StudentRemovalService(
			UserRepository users,
			TeacherStudentRepository assignments,
			PasswordLinkService links) {
		this.users = users;
		this.assignments = assignments;
		this.links = links;
	}

	@Transactional
	public void removeStudent(UUID teacherId, UUID studentId) {
		// A concurrent reset by the same teacher takes this lock too; without it the reset
		// could insert a link after the removal voided the links, and the link would survive.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!assignments.delete(teacherId, studentId)) {
			throw ApiException.notFound();
		}
		links.invalidateLinks(studentId, teacherId);
	}
}
