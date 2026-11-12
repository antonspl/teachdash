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
public class StudentResetLinkService {

	private final UserRepository users;
	private final TeacherStudentRepository assignments;
	private final PasswordLinkService links;

	StudentResetLinkService(
			UserRepository users,
			TeacherStudentRepository assignments,
			PasswordLinkService links) {
		this.users = users;
		this.assignments = assignments;
		this.links = links;
	}

	@Transactional
	public String issueResetLink(UUID teacherId, UUID studentId) {
		// Removal of this student by the same teacher takes this lock too, so the assignment
		// checked below cannot disappear before the link is stored.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!assignments.exists(teacherId, studentId)) {
			throw ApiException.notFound();
		}
		return links.issueResetLink(studentId, teacherId);
	}
}
