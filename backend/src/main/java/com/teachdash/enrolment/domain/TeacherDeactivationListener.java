package com.teachdash.enrolment.domain;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.accounts.domain.TeacherDeactivated;
import com.teachdash.accounts.domain.TokenPurpose;
import com.teachdash.enrolment.persistence.TeacherJoinLinkRepository;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

/** Runs synchronously in the deactivation transaction, so a failure rolls it back. */
@Component
class TeacherDeactivationListener {

	private final TeacherStudentRepository assignments;
	private final TeacherJoinLinkRepository joinLinks;
	private final PasswordLinkService links;

	TeacherDeactivationListener(
			TeacherStudentRepository assignments,
			TeacherJoinLinkRepository joinLinks,
			PasswordLinkService links) {
		this.assignments = assignments;
		this.joinLinks = joinLinks;
		this.links = links;
	}

	@EventListener
	void onTeacherDeactivated(TeacherDeactivated event) {
		joinLinks.delete(event.teacherId());
		for (var studentId : assignments.deleteAllOfTeacher(event.teacherId())) {
			links.invalidateLinks(studentId, TokenPurpose.SET_PASSWORD);
		}
	}
}
