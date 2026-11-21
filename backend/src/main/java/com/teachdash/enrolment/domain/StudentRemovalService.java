package com.teachdash.enrolment.domain;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.AssignmentEnding;

@Service
public class StudentRemovalService {

	private final UserRepository users;

	private final AssignmentEnding ending;

	StudentRemovalService(UserRepository users, AssignmentEnding ending) {
		this.users = users;
		this.ending = ending;
	}

	@Transactional
	public void removeStudent(UUID teacherId, UUID studentId) {
		// A concurrent reset by the same teacher takes this lock too; without it the reset
		// could insert a link after the removal voided the links, and the link would survive.
		// A teacher deactivated after the session check is not found here and gets 401.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!ending.end(teacherId, studentId)) {
			throw ApiException.notFound();
		}
	}
}
