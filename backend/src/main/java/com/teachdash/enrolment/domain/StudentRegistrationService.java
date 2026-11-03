package com.teachdash.enrolment.domain;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.domain.AccountService;
import com.teachdash.accounts.domain.CreatedAccount;
import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class StudentRegistrationService {

	private final UserRepository users;
	private final TeacherStudentRepository assignments;
	private final AccountService accounts;
	private final Clock clock;

	StudentRegistrationService(
			UserRepository users,
			TeacherStudentRepository assignments,
			AccountService accounts,
			Clock clock) {
		this.users = users;
		this.assignments = assignments;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional
	public CreatedAccount register(UUID teacherId, String name, String email, String timeZone) {
		Optional<User> existing = users.findByEmailIgnoreCase(email);
		if (existing.isPresent()) {
			throw conflictFor(teacherId, existing.get());
		}
		CreatedAccount created = accounts.createPasswordlessUser(Role.STUDENT, name, email, timeZone);
		assignments.insert(teacherId, created.id(), clock.instant());
		return created;
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
}
