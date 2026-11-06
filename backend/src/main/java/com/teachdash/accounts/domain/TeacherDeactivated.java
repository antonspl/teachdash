package com.teachdash.accounts.domain;

import java.util.UUID;

/** Published inside the deactivation transaction, after the teacher is deactivated. */
public record TeacherDeactivated(
		UUID teacherId) {
}
