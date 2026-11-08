package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** R10. */
class StudentJoinLinkTest extends ApiTest {

	private static final String JOIN = "/api/join-links/join";
	private static final String STUDENTS = "/api/teacher/students";

	private long userCount() {
		return jdbc.sql("select count(*) from users").query(Long.class).single();
	}

	private long teacherCountOf(UUID student) {
		return jdbc.sql("select count(*) from teacher_students where student_id = :id")
				.param("id", student)
				.query(Long.class)
				.single();
	}

	@Test
	@DisplayName("[student-enrolment/R10-S1] Student with no teacher is assigned")
	void studentWithNoTeacherIsAssigned() {
		// given teacher A with a join link, and a logged-in student with no teacher, name "Анна", time zone "Europe/Moscow" and password "old-password-1"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		UUID student = createUser("STUDENT", "Анна", "anna@example.com", "old-password-1", true);
		jdbc.sql("update users set time_zone = 'Europe/Moscow' where id = :id").param("id", student).update();
		TestClient teacher = loggedInAs("a@example.com");
		TestClient studentClient = loggedInAs("anna@example.com");
		long usersBefore = userCount();

		// when the student joins through the link, sending also the name "Anna" and the email "other@example.com"
		var response = studentClient.post(JOIN, Map.of(
				"token", token,
				"name", "Anna",
				"email", "other@example.com"));

		// then the response is 200
		response.then().statusCode(200).body("teacherId", equalTo(teacherA.toString()));
		// and the student is in teacher A's list with the name "Анна"
		teacher.get(STUDENTS).then().statusCode(200)
				.body("id", contains(student.toString()))
				.body("name", contains("Анна"));
		// and the email and time zone are unchanged
		var stored = jdbc.sql("select email || '|' || time_zone from users where id = :id")
				.param("id", student)
				.query(String.class)
				.single();
		assertEquals("anna@example.com|Europe/Moscow", stored);
		// and teacher A is in the student's list of teachers
		studentClient.get("/api/student/teachers").then().statusCode(200)
				.body("id", contains(teacherA.toString()));
		// and no account is created, and the student logs in with "old-password-1"
		assertEquals(usersBefore, userCount());
		newClient().login("anna@example.com", "old-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R10-S2] Student of another teacher is rejected")
	void studentOfAnotherTeacherIsRejected() {
		// given teacher A with a join link, and a logged-in student assigned to teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		UUID student = createStudent("anna@example.com", "student-pass-1");
		assignStudent(teacherB, student);
		TestClient studentClient = loggedInAs("anna@example.com");

		// when the student joins through teacher A's link
		var response = studentClient.post(JOIN, Map.of("token", token));

		// then the response is 409 with code STUDENT_EXISTS
		response.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
		// and the student is still in teacher B's list, and teacher A's list is empty
		loggedInAs("b@example.com").get(STUDENTS).then().statusCode(200).body("id", contains(student.toString()));
		loggedInAs("a@example.com").get(STUDENTS).then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[student-enrolment/R10-S3] Student of the same teacher is rejected")
	void studentOfTheSameTeacherIsRejected() {
		// given teacher A with a join link, and a logged-in student assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		UUID student = createStudent("anna@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient studentClient = loggedInAs("anna@example.com");

		// when the student joins through the link
		var response = studentClient.post(JOIN, Map.of("token", token));

		// then the response is 409 with code ALREADY_ENROLLED
		response.then().statusCode(409).body("code", equalTo("ALREADY_ENROLLED"));
		// and there is still exactly one assignment between them
		assertEquals(1, jdbc.sql("select count(*) from teacher_students where teacher_id = :t and student_id = :s")
				.param("t", teacherA)
				.param("s", student)
				.query(Long.class)
				.single());
	}

	@Test
	@DisplayName("[student-enrolment/R10-S4] Removed student can join again")
	void removedStudentCanJoinAgain() {
		// given teacher A with a join link, and a logged-in student assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		UUID student = createStudent("anna@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient studentClient = loggedInAs("anna@example.com");

		// when teacher A removes the student, and the student then joins through the link
		teacher.delete(STUDENTS + "/" + student).then().statusCode(204);
		var response = studentClient.post(JOIN, Map.of("token", token));

		// then the join response is 200
		response.then().statusCode(200);
		// and the student is in teacher A's list
		teacher.get(STUDENTS).then().statusCode(200).body("id", contains(student.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R10-S5] Parallel joins assign the student once")
	void parallelJoinsAssignTheStudentOnce() throws Exception {
		// given teachers A and B, each with a join link, and a logged-in student with no teacher
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		String tokenA = createJoinLink(teacherA);
		String tokenB = createJoinLink(teacherB);
		UUID student = createStudent("anna@example.com", "student-pass-1");
		int requests = 8;
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			TestClient client = loggedInAs("anna@example.com");
			String token = i % 2 == 0 ? tokenA : tokenB;
			calls.add(() -> client.post(JOIN, Map.of("token", token)));
		}

		// when the student sends several parallel join requests through both links
		List<Response> results = Parallel.run(calls);

		// then exactly one response is 200
		List<Integer> codes = results.stream().map(Response::statusCode).toList();
		assertEquals(1, codes.stream().filter(code -> code == 200).count(), codes.toString());
		// and the other responses through the winning teacher's link are 409 with code ALREADY_ENROLLED,
		// and those through the other link are 409 with code STUDENT_EXISTS
		int winner = codes.indexOf(200) % 2;
		for (int i = 0; i < requests; i++) {
			if (codes.get(i) == 200) {
				continue;
			}
			String expected = i % 2 == winner ? "ALREADY_ENROLLED" : "STUDENT_EXISTS";
			assertEquals(409, codes.get(i), codes.toString());
			assertEquals(expected, results.get(i).path("code"), codes.toString());
		}
		// and the student has exactly one teacher
		assertEquals(1, teacherCountOf(student));
	}

	@Test
	@DisplayName("[student-enrolment/R10-S6] Invalid link changes nothing")
	void invalidLinkChangesNothing() {
		// given teacher A whose join link was disabled, teacher B with a join link who was then deactivated, and a logged-in student with no teacher
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String disabled = createJoinLink(teacherA);
		jdbc.sql("delete from teacher_join_links where teacher_id = :id").param("id", teacherA).update();
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		String deactivated = createJoinLink(teacherB);
		loggedInAdmin().post("/api/admin/teachers/" + teacherB + "/deactivate").then().statusCode(204);
		UUID student = createStudent("anna@example.com", "student-pass-1");
		TestClient studentClient = loggedInAs("anna@example.com");

		// when the student joins through teacher A's disabled link and through teacher B's link
		var viaDisabled = studentClient.post(JOIN, Map.of("token", disabled));
		var viaDeactivated = studentClient.post(JOIN, Map.of("token", deactivated));

		// then both responses are 410 with code LINK_INVALID
		viaDisabled.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		viaDeactivated.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the student still has no teacher
		assertEquals(0, teacherCountOf(student));
	}
}
