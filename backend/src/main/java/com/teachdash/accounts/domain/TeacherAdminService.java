package com.teachdash.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
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
	private final ApplicationEventPublisher events;

	TeacherAdminService(
			UserRepository users,
			AccountService accounts,
			PasswordLinkService links,
			ApplicationEventPublisher events) {
		this.users = users;
		this.accounts = accounts;
		this.links = links;
		this.events = events;
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
	 * Deactivates the teacher and revokes their sessions (session version), then invalidates
	 * their unused links. The user row is always locked before its links.
	 */
	@Transactional
	public void deactivateTeacher(UUID id) {
		if (!users.existsByIdAndRole(id, Role.TEACHER)) {
			throw notFound();
		}
		users.deactivate(id);
		links.invalidateLinks(id);
		events.publishEvent(new TeacherDeactivated(id));
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

	/**
	 * Returns a reset link for the user. Rejects the admin's own account and inactive accounts
	 * before anything is issued, so a rejected request leaves existing links untouched. The user
	 * row is locked first, so a concurrent deactivation is ordered against this request (R8).
	 */
	@Transactional
	public String issueResetLink(UUID userId, UUID currentUserId) {
		User user = users.findByIdForUpdate(userId).orElseThrow(TeacherAdminService::notFound);
		if (user.getId().equals(currentUserId)) {
			throw ownAccount();
		}
		if (!user.isActive()) {
			throw accountDeactivated();
		}
		return links.issue(user.getId(), TokenPurpose.RESET_PASSWORD);
	}

	private static ApiException ownAccount() {
		return new ApiException(HttpStatus.CONFLICT, "OWN_ACCOUNT");
	}

	private static ApiException accountDeactivated() {
		return new ApiException(HttpStatus.CONFLICT, "ACCOUNT_DEACTIVATED");
	}

	private static ApiException emailTaken() {
		return new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
	}
}
