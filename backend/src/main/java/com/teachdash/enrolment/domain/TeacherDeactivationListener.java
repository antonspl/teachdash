package com.teachdash.enrolment.domain;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.teachdash.accounts.domain.TeacherDeactivated;
import com.teachdash.enrolment.persistence.AssignmentEnding;
import com.teachdash.enrolment.persistence.TeacherJoinLinkRepository;

/** Runs synchronously in the deactivation transaction, so a failure rolls it back. */
@Component
class TeacherDeactivationListener {

	private final TeacherJoinLinkRepository joinLinks;
	private final AssignmentEnding ending;

	TeacherDeactivationListener(TeacherJoinLinkRepository joinLinks, AssignmentEnding ending) {
		this.joinLinks = joinLinks;
		this.ending = ending;
	}

	@EventListener
	void onTeacherDeactivated(TeacherDeactivated event) {
		joinLinks.delete(event.teacherId());
		ending.endAll(event.teacherId());
	}
}
