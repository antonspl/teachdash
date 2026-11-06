package com.teachdash.enrolment.api.admin;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R7. */
class TeacherDeactivationTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";
	private static final String TEACHERS = "/api/student/teachers";

	private String deactivate(UUID teacher) {
		return "/api/admin/teachers/" + teacher + "/deactivate";
	}

	@Test
	@DisplayName("[student-enrolment/R7-S1] Deactivation releases every student")
	void deactivationReleasesEveryStudent() {
		// given teacher A with a student S1 who has a password and a student S2 without a password with an unused set-password link, and teacher B with a student S3 without a password with an unused set-password link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID s1 = createStudent("s1@example.com", "student-pass-1");
		UUID s2 = createUser("STUDENT", "Student 2", "s2@example.com", null, true);
		UUID s3 = createUser("STUDENT", "Student 3", "s3@example.com", null, true);
		assignStudent(teacherA, s1);
		assignStudent(teacherA, s2);
		assignStudent(teacherB, s3);
		String tokenS2 = createToken(s2, "SET_PASSWORD", false);
		String tokenS3 = createToken(s3, "SET_PASSWORD", false);
		TestClient admin = loggedInAdmin();
		TestClient studentOne = loggedInAs("s1@example.com");
		TestClient teacherBClient = loggedInAs("b@example.com");

		// when the admin deactivates teacher A, the link of S2 is then used to set the password "new-password-1", and the link of S3 to set the password "new-password-3"
		var deactivation = admin.post(deactivate(teacherA));
		var linkS2 = newClient().post("/api/password-links", Map.of("token", tokenS2, "password", "new-password-1"));
		var linkS3 = newClient().post("/api/password-links", Map.of("token", tokenS3, "password", "new-password-3"));

		// then the deactivation response is 204
		deactivation.then().statusCode(204);
		// and the admin's list of teacher A's students is empty
		admin.get("/api/admin/teachers/" + teacherA + "/students").then().statusCode(200).body("$", empty());
		// and S1's list of teachers is empty
		studentOne.get(TEACHERS).then().statusCode(200).body("$", empty());
		// and S1 can still log in
		newClient().login("s1@example.com", "student-pass-1").then().statusCode(200);
		// and the response for S2's link is 410 with code LINK_INVALID
		linkS2.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and S3 is still in teacher B's list, and S3 logs in with "new-password-3"
		teacherBClient.get(STUDENTS).then().statusCode(200).body("id", contains(s3.toString()));
		linkS3.then().statusCode(200);
		newClient().login("s3@example.com", "new-password-3").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R7-S2] Released student can be registered by another teacher")
	void releasedStudentCanBeRegisteredByAnotherTeacher() {
		// given teacher A with a student who has a password, and teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient admin = loggedInAdmin();
		TestClient b = loggedInAs("b@example.com");
		TestClient studentClient = loggedInAs("student@example.com");

		// when the admin deactivates teacher A, and teacher B then registers a student with that email
		var deactivation = admin.post(deactivate(teacherA));
		var registration = b.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "UTC"));

		// then the registration response is 201
		deactivation.then().statusCode(204);
		registration.then().statusCode(201);
		// and the student is in teacher B's list
		b.get(STUDENTS).then().statusCode(200).body("id", contains(student.toString()));
		// and teacher B is the only entry in the student's list of teachers
		studentClient.get(TEACHERS).then().statusCode(200).body("id", contains(teacherB.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R7-S3] Reactivation does not restore students")
	void reactivationDoesNotRestoreStudents() {
		// given teacher A with a password and a student
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient admin = loggedInAdmin();
		TestClient studentClient = loggedInAs("student@example.com");

		// when the admin deactivates and then reactivates teacher A
		var deactivation = admin.post(deactivate(teacherA));
		var reactivation = admin.post("/api/admin/teachers/" + teacherA + "/reactivate");

		// then the admin's list of teacher A's students is empty
		deactivation.then().statusCode(204);
		reactivation.then().statusCode(204);
		admin.get("/api/admin/teachers/" + teacherA + "/students").then().statusCode(200).body("$", empty());
		// and the student's list of teachers is empty
		studentClient.get(TEACHERS).then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[student-enrolment/R7-S5] Release keeps an admin's reset link")
	void releaseKeepsAnAdminsResetLink() {
		// given teacher A with a student who has a password and an unused reset link issued by an admin
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		String token = createToken(student, "RESET_PASSWORD", false);
		TestClient admin = loggedInAdmin();

		// when the admin deactivates teacher A and the link is then used to set the password "new-password-1"
		var deactivation = admin.post(deactivate(teacherA));
		var link = newClient().post("/api/password-links", Map.of("token", token, "password", "new-password-1"));

		// then the deactivation response is 204, the link response is 200, and the student logs in with "new-password-1"
		deactivation.then().statusCode(204);
		link.then().statusCode(200);
		newClient().login("student@example.com", "new-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R7-S6] Teacher without students and repeated deactivation")
	void teacherWithoutStudentsAndRepeatedDeactivation() {
		// given teacher A with no students, and teacher B with a student
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherB, student);
		TestClient admin = loggedInAdmin();
		TestClient b = loggedInAs("b@example.com");

		// when the admin deactivates teacher A twice
		var first = admin.post(deactivate(teacherA));
		var second = admin.post(deactivate(teacherA));

		// then both responses are 204
		first.then().statusCode(204);
		second.then().statusCode(204);
		// and the student is still in teacher B's list
		b.get(STUDENTS).then().statusCode(200).body("id", contains(student.toString()));
	}
}
