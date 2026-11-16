package com.teachdash.enrolment.domain;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.enrolment.persistence.TeacherStudentRepository;

@Service
public class StudentLanguageService {

	private final UserRepository users;

	private final TeacherStudentRepository assignments;

	StudentLanguageService(UserRepository users, TeacherStudentRepository assignments) {
		this.users = users;
		this.assignments = assignments;
	}

	@Transactional
	public UserSummary change(UUID teacherId, UUID studentId, String nativeLanguage) {
		// Removal of this student by the same teacher takes this lock too, so the assignment
		// checked below cannot disappear before the update. A teacher deactivated after the
		// session check is not found here and gets 401.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		UserSummary student = assignments.findStudent(teacherId, studentId)
				.orElseThrow(ApiException::notFound);
		users.updateNativeLanguage(studentId, nativeLanguage);
		return new UserSummary(
				student.id(),
				student.name(),
				student.email(),
				student.timeZone(),
				student.active(),
				nativeLanguage);
	}
}
