package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R4. */
class StudentRemovalTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";
	private static final String TEACHERS = "/api/student/teachers";

	private UUID teacher(String name, String email) {
		return createUser("TEACHER", name, email, "teacher-pass-1", true);
	}

	@Test
	@DisplayName("[student-enrolment/R4-S1] Removed student disappears from the lists")
	void removedStudentDisappearsFromTheLists() {
		// given a student with a password, assigned to teacher A
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient studentClient = loggedInAs("student@example.com");

		// when teacher A removes the student
		var response = teacher.delete(STUDENTS + "/" + student);

		// then the response is 204, the student is not in teacher A's list, teacher A is not in the student's list
		response.then().statusCode(204);
		teacher.get(STUDENTS).then().statusCode(200).body("id", not(hasItem(student.toString())));
		studentClient.get(TEACHERS).then().statusCode(200).body("id", not(hasItem(teacherA.toString())));
		// and the student can still log in
		newClient().login("student@example.com", "student-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R4-S2] Assignments to other teachers stay")
	void assignmentsToOtherTeachersStay() {
		// given a student assigned to teachers A and B
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID teacherB = teacher("Teacher B", "b@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		assignStudent(teacherB, student);
		TestClient a = loggedInAs("a@example.com");
		TestClient b = loggedInAs("b@example.com");
		TestClient studentClient = loggedInAs("student@example.com");

		// when teacher A removes the student
		var response = a.delete(STUDENTS + "/" + student);

		// then the response is 204, the student is still in teacher B's list, and teacher B is in the student's list
		response.then().statusCode(204);
		b.get(STUDENTS).then().statusCode(200).body("id", hasItem(student.toString()));
		studentClient.get(TEACHERS).then().statusCode(200).body("id", contains(teacherB.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S3] Last removal keeps the account and the unused link")
	void lastRemovalKeepsTheAccountAndTheUnusedLink() {
		// given a student without a password, assigned only to teacher A, with an unused set-password link
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createUser("STUDENT", "Student", "student@example.com", null, true);
		assignStudent(teacherA, student);
		String token = createToken(student, "SET_PASSWORD", false);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient holder = newClient();

		// when teacher A removes the student and the link is then used to set the password "new-password-1"
		var removal = teacher.delete(STUDENTS + "/" + student);
		holder.post("/api/password-links", Map.of("token", token, "password", "new-password-1"));

		// then the removal response is 204, the student logs in with "new-password-1", and their list of teachers is empty
		removal.then().statusCode(204);
		TestClient loggedIn = newClient();
		loggedIn.login("student@example.com", "new-password-1").then().statusCode(200);
		loggedIn.get(TEACHERS).then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[student-enrolment/R4-S4] Another teacher's student is not found")
	void anotherTeachersStudentIsNotFound() {
		// given a student assigned only to teacher B
		teacher("Teacher A", "a@example.com");
		UUID teacherB = teacher("Teacher B", "b@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherB, student);
		TestClient a = loggedInAs("a@example.com");
		TestClient b = loggedInAs("b@example.com");

		// when teacher A removes that student, and then an id with no account
		var foreign = a.delete(STUDENTS + "/" + student);
		var unknown = a.delete(STUDENTS + "/" + UUID.randomUUID());

		// then both responses are 404 with code NOT_FOUND, and the student is still in teacher B's list
		foreign.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		unknown.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		b.get(STUDENTS).then().statusCode(200).body("id", hasItem(student.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S5] Removing twice is not found")
	void removingTwiceIsNotFound() {
		// given a student assigned to teacher A
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A removes the student twice
		var first = teacher.delete(STUDENTS + "/" + student);
		var second = teacher.delete(STUDENTS + "/" + student);

		// then the first response is 204 and the second is 404 with code NOT_FOUND
		first.then().statusCode(204);
		second.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S6] Malformed id is not found")
	void malformedIdIsNotFound() {
		// given a logged-in teacher
		teacher("Teacher A", "a@example.com");
		TestClient teacher = loggedInAs("a@example.com");

		// when the teacher removes the id "not-a-uuid"
		var response = teacher.delete(STUDENTS + "/not-a-uuid");

		// then the response is 404 with code NOT_FOUND
		response.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S7] Removed student cannot be registered again")
	void removedStudentCannotBeRegisteredAgain() {
		// given a student assigned to teacher A
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A removes the student and then registers a student with the same email
		var removal = teacher.delete(STUDENTS + "/" + student);
		var registration = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "UTC"));

		// then the removal response is 204, the registration response is 409 with code STUDENT_EXISTS
		removal.then().statusCode(204);
		registration.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
		// and the student is not in teacher A's list
		teacher.get(STUDENTS).then().statusCode(200).body("id", not(hasItem(student.toString())));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S8] Students and admins cannot remove")
	void studentsAndAdminsCannotRemove() {
		// given a student assigned to teacher A, a logged-in student and a logged-in admin
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient studentClient = loggedInAs("student@example.com");
		TestClient admin = loggedInAdmin();

		// when the logged-in student and the admin each remove that student through the teacher endpoint
		var byStudent = studentClient.delete(STUDENTS + "/" + student);
		var byAdmin = admin.delete(STUDENTS + "/" + student);

		// then both responses are 403, and the student is still in teacher A's list
		byStudent.then().statusCode(403);
		byAdmin.then().statusCode(403);
		teacher.get(STUDENTS).then().statusCode(200).body("id", hasItem(student.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S9] Removing requires a session")
	void removingRequiresASession() {
		// given a student assigned to teacher A and a client with a valid CSRF token and no session
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient anonymous = newClient();

		// when the client removes that student
		var response = anonymous.delete(STUDENTS + "/" + student);

		// then the response is 401 with code UNAUTHENTICATED, and the student is still in teacher A's list
		response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		teacher.get(STUDENTS).then().statusCode(200).body("id", hasItem(student.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R4-S10] Request without a CSRF token is rejected")
	void requestWithoutCsrfTokenIsRejected() {
		// given a student assigned to teacher A
		UUID teacherA = teacher("Teacher A", "a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A removes the student without a CSRF token
		var response = teacher.snapshot().withoutCsrfHeader().delete(STUDENTS + "/" + student);

		// then the response is 403 with code CSRF_INVALID, and the student is still in teacher A's list
		response.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		teacher.get(STUDENTS).then().statusCode(200).body("id", hasItem(student.toString()));
	}
}
