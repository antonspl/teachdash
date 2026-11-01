package com.teachdash.accounts.domain;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
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
	private final Clock clock;

	TeacherAdminService(
			UserRepository users,
			PasswordLinkService links,
			Clock clock) {
		this.users = users;
		this.links = links;
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
			teacher = users.save(User.create(
					normalized,
					name.strip(),
					Role.TEACHER,
					null,
					timeZone,
					clock.instant()));
		} catch (DuplicateKeyException e) {
			throw emailTaken();
		}
		return new CreatedTeacher(teacher.getId(), links.issue(teacher.getId(), TokenPurpose.SET_PASSWORD));
	}

	/**
	 * Revokes the teacher's sessions (session version) and unused links, then deactivates.
	 * Rows are always locked in the same order: links first, then the user.
	 */
	@Transactional
	public void deactivateTeacher(UUID id) {
		requireTeacher(id);
		links.invalidateLinks(id);
		users.deactivate(id);
	}

	/** Returns a new set-password link when the teacher never set a password. */
	@Transactional
	public Optional<String> reactivateTeacher(UUID id) {
		User teacher = requireTeacher(id);
		Optional<String> link = teacher.getPasswordHash() == null
				? Optional.of(links.issue(id, TokenPurpose.SET_PASSWORD))
				: Optional.empty();
		users.reactivate(id);
		return link;
	}

	private User requireTeacher(UUID id) {
		return users.findById(id)
				.filter(user -> user.getRole() == Role.TEACHER)
				.orElseThrow(TeacherAdminService::notFound);
	}

	public String issueResetLink(UUID userId) {
		User user = users.findById(userId).orElseThrow(TeacherAdminService::notFound);
		return links.issue(user.getId(), TokenPurpose.RESET_PASSWORD);
	}

	public record CreatedTeacher(
			UUID id,
			String setPasswordLink) {
	}

	private static ApiException emailTaken() {
		return new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
	}
}
