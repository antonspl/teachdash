package com.teachdash.enrolment.domain;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.teachdash.accounts.domain.AccountService;
import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherJoinLinkRepository;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class JoinLinkService {

	private final UserRepository users;
	private final TeacherJoinLinkRepository joinLinks;
	private final TeacherStudentRepository assignments;
	private final AccountService accounts;
	private final Clock clock;
	private final TransactionTemplate transaction;

	JoinLinkService(
			UserRepository users,
			TeacherJoinLinkRepository joinLinks,
			TeacherStudentRepository assignments,
			AccountService accounts,
			Clock clock,
			PlatformTransactionManager transactionManager) {
		this.users = users;
		this.joinLinks = joinLinks;
		this.assignments = assignments;
		this.accounts = accounts;
		this.clock = clock;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	@Transactional(readOnly = true)
	public String previewTeacherName(String token) {
		UUID teacherId = joinLinks.findTeacherId(token).orElseThrow(JoinLinkService::invalidLink);
		return users.findById(teacherId)
				.filter(User::isActive)
				.orElseThrow(JoinLinkService::invalidLink)
				.getName();
	}

	/** Bcrypt runs before the transaction starts, so no database connection is held while hashing. */
	public User joinAsVisitor(String token, String name, String email, String timeZone, String password) {
		String passwordHash = accounts.hashPassword(password);
		return transaction.execute(status -> {
			UUID teacherId = lockTeacher(token);
			User student = accounts.createUserWithPassword(Role.STUDENT, name, email, timeZone, passwordHash);
			if (!assignments.insert(teacherId, student.getId(), clock.instant())) {
				throw new IllegalStateException("New student already has a teacher");
			}
			return student;
		});
	}

	@Transactional
	public UUID joinAsStudent(String token, UUID studentId) {
		UUID teacherId = lockTeacher(token);
		users.findByIdForUpdate(studentId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
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
		return teacherId;
	}

	/**
	 * Finds the link's teacher and locks the teacher row. A parallel deactivation or link
	 * regeneration updates or waits on this row, so after the lock the link is read again: if it
	 * changed or is gone, the join is refused instead of using a link that was just replaced.
	 */
	private UUID lockTeacher(String token) {
		UUID teacherId = joinLinks.findTeacherId(token).orElseThrow(JoinLinkService::invalidLink);
		users.findActiveByIdForUpdate(teacherId).orElseThrow(JoinLinkService::invalidLink);
		if (!joinLinks.findTeacherId(token).filter(teacherId::equals).isPresent()) {
			throw invalidLink();
		}
		return teacherId;
	}

	private static ApiException invalidLink() {
		return new ApiException(HttpStatus.GONE, "LINK_INVALID");
	}
}
