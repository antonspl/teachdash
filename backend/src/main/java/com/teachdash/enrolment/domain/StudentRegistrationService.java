package com.teachdash.enrolment.domain;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.domain.AccountService;
import com.teachdash.accounts.domain.CreatedAccount;
import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.TokenPurpose;
import com.teachdash.accounts.domain.User;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class StudentRegistrationService {

	private final UserRepository users;
	private final TeacherStudentRepository assignments;
	private final AccountService accounts;
	private final PasswordLinkService links;
	private final Clock clock;

	StudentRegistrationService(
			UserRepository users,
			TeacherStudentRepository assignments,
			AccountService accounts,
			PasswordLinkService links,
			Clock clock) {
		this.users = users;
		this.assignments = assignments;
		this.accounts = accounts;
		this.links = links;
		this.clock = clock;
	}

	@Transactional
	public CreatedAccount register(UUID teacherId, String name, String email, String timeZone) {
		// A parallel deactivation updates this row, so it either finishes first (401 here)
		// or waits until this registration commits and then releases the new student too.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		Optional<User> existing = users.findByEmailIgnoreCase(email);
		if (existing.isEmpty()) {
			CreatedAccount created = accounts.createPasswordlessUser(Role.STUDENT, name, email, timeZone, teacherId);
			assignments.insert(teacherId, created.id(), clock.instant());
			return created;
		}
		if (existing.get().getRole() != Role.STUDENT) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
		}
		// Re-read under the lock so the password hash is current.
		User student = users.findByIdForUpdate(existing.get().getId()).orElseThrow();
		return assignExisting(teacherId, student);
	}

	private CreatedAccount assignExisting(UUID teacherId, User student) {
		UUID studentId = student.getId();
		if (assignments.exists(teacherId, studentId)) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ENROLLED");
		}
		if (assignments.hasTeacher(studentId)) {
			throw new ApiException(HttpStatus.CONFLICT, "STUDENT_EXISTS");
		}
		if (!assignments.insert(teacherId, studentId, clock.instant())) {
			String code = assignments.exists(teacherId, studentId)
					? "ALREADY_ENROLLED"
					: "STUDENT_EXISTS";
			throw new ApiException(HttpStatus.CONFLICT, code);
		}
		String link = student.getPasswordHash() == null
				? links.issue(studentId, TokenPurpose.SET_PASSWORD, teacherId)
				: null;
		return new CreatedAccount(studentId, link);
	}
}
