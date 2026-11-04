package com.teachdash.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;

/** Admin operations on teacher accounts (R5, R6, R8). */
@Service
public class TeacherAdminService {

	private final UserRepository users;
	private final AccountService accounts;
	private final PasswordLinkService links;

	TeacherAdminService(
			UserRepository users,
			AccountService accounts,
			PasswordLinkService links) {
		this.users = users;
		this.accounts = accounts;
		this.links = links;
	}

	public List<User> listTeachers() {
		return users.findByRoleOrderByName(Role.TEACHER);
	}

	/** Creates a teacher without a password; returns the id and the set-password link. */
	@Transactional
	public CreatedAccount createTeacher(String name, String email, String timeZone) {
		if (users.findByEmailIgnoreCase(email).isPresent()) {
			throw emailTaken();
		}
		return accounts.createPasswordlessUser(Role.TEACHER, name, email, timeZone);
	}

	/**
	 * Revokes the teacher's sessions (session version) and unused links, then deactivates.
	 * Rows are always locked in the same order: links first, then the user.
	 */
	@Transactional
	public void deactivateTeacher(UUID id) {
		if (!users.existsByIdAndRole(id, Role.TEACHER)) {
			throw notFound();
		}
		links.invalidateLinks(id);
		users.deactivate(id);
	}

	/** Returns a new set-password link when an inactive teacher never set a password. */
	@Transactional
	public Optional<String> reactivateTeacher(UUID id) {
		User teacher = requireTeacher(id);
		if (teacher.isActive()) {
			return Optional.empty();
		}
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

	private static ApiException emailTaken() {
		return new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
	}
}
