package com.teachdash.accounts.domain;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;

/** Admin operations on teacher accounts (R5, R6, R8). */
@Service
public class TeacherAdminService {

	private final UserRepository users;
	private final PasswordLinkService links;
	private final AccountService accounts;
	private final Clock clock;

	TeacherAdminService(UserRepository users, PasswordLinkService links, AccountService accounts, Clock clock) {
		this.users = users;
		this.links = links;
		this.accounts = accounts;
		this.clock = clock;
	}

	public List<User> listTeachers() {
		return users.findByRoleOrderByName(Role.TEACHER);
	}

	/** Creates a teacher without a password; returns the id and the set-password link. */
	@Transactional
	public CreatedTeacher createTeacher(String name, String email, String timeZone) {
		AccountService.requireValidTimeZone(timeZone);
		String normalized = email.strip();
		if (users.findByEmailIgnoreCase(normalized).isPresent()) {
			throw emailTaken();
		}
		User teacher;
		try {
			teacher = users.save(User.create(normalized, name.strip(), Role.TEACHER, null, timeZone, clock.instant()));
		} catch (DuplicateKeyException e) {
			throw emailTaken();
		}
		return new CreatedTeacher(teacher.getId(), links.issue(teacher.getId(), TokenPurpose.SET_PASSWORD));
	}

	/** Deactivation also ends the teacher's sessions. */
	@Transactional
	public void setTeacherActive(UUID id, boolean active) {
		User teacher = users.findById(id).filter(u -> u.getRole() == Role.TEACHER).orElseThrow(TeacherAdminService::notFound);
		teacher.setActive(active);
		users.save(teacher);
		if (!active) {
			accounts.endSessions(id);
		}
	}

	public String issueResetLink(UUID userId) {
		User user = users.findById(userId).orElseThrow(TeacherAdminService::notFound);
		return links.issue(user.getId(), TokenPurpose.RESET_PASSWORD);
	}

	public record CreatedTeacher(UUID id, String setPasswordLink) {
	}

	private static ApiException emailTaken() {
		return new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
	}
}
