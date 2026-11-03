package com.teachdash.enrolment.domain;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final PasswordLinkService links;
	private final Clock clock;

	StudentRegistrationService(
			UserRepository users,
			TeacherStudentRepository assignments,
			PasswordLinkService links,
			Clock clock) {
		this.users = users;
		this.assignments = assignments;
		this.links = links;
		this.clock = clock;
	}

	/**
	 * Creates the student, the assignment and the set-password link in one transaction.
	 * A lost race on the email unique index surfaces from the insert as a 409 that rolls everything back.
	 */
	@Transactional
	public RegisteredStudent register(UUID teacherId, String name, String email, String timeZone) {
		String normalized = email.strip();
		Optional<User> existing = users.findByEmailIgnoreCase(normalized);
		if (existing.isPresent()) {
			throw conflictFor(teacherId, existing.get());
		}
		User student;
		try {
			student = users.save(User.create(
					normalized,
					name.strip(),
					Role.STUDENT,
					null,
					timeZone,
					clock.instant()));
		} catch (DuplicateKeyException e) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
		}
		assignments.insert(teacherId, student.getId(), clock.instant());
		return new RegisteredStudent(
				student.getId(),
				links.issue(student.getId(), TokenPurpose.SET_PASSWORD));
	}

	private ApiException conflictFor(UUID teacherId, User existing) {
		if (existing.getRole() != Role.STUDENT) {
			return new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
		}
		String code = assignments.exists(teacherId, existing.getId())
				? "ALREADY_ENROLLED"
				: "STUDENT_EXISTS";
		return new ApiException(HttpStatus.CONFLICT, code);
	}

	public record RegisteredStudent(
			UUID id,
			String setPasswordLink) {
	}
}
