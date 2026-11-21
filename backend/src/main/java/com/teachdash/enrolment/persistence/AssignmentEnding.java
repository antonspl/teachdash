package com.teachdash.enrolment.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.teachdash.accounts.domain.PasswordLinkService;

/**
 * The only way to delete from {@code teacher_students}: ending an assignment also voids the
 * unused links the teacher issued for the student. Runs in the caller's transaction, and the
 * teacher row must already be locked there ({@code select ... for update}); this class does
 * not lock.
 */
@Component
public class AssignmentEnding {

	private final TeacherStudentRepository assignments;

	private final PasswordLinkService links;

	AssignmentEnding(TeacherStudentRepository assignments, PasswordLinkService links) {
		this.assignments = assignments;
		this.links = links;
	}

	/**
	 * Ends one assignment and voids the teacher's unused links for the student.
	 * The teacher row must be locked in the current transaction.
	 *
	 * @return whether the assignment existed; nothing is voided when it did not
	 */
	public boolean end(UUID teacherId, UUID studentId) {
		if (!assignments.delete(teacherId, studentId)) {
			return false;
		}
		links.invalidateLinksIssuedBy(Set.of(studentId), teacherId);
		return true;
	}

	/**
	 * Ends every assignment of the teacher and voids the teacher's unused links for those
	 * students in one statement. The teacher row must be locked in the current transaction.
	 */
	public void endAll(UUID teacherId) {
		List<UUID> studentIds = assignments.deleteAllOfTeacher(teacherId);
		links.invalidateLinksIssuedBy(studentIds, teacherId);
	}
}
