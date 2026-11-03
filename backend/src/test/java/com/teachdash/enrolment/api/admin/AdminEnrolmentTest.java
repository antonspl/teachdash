package com.teachdash.enrolment.api.admin;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

class AdminEnrolmentTest extends ApiTest {

	@Test
	@DisplayName("[student-enrolment/R3-S1] Admin lists a teacher's students")
	void adminListsATeachersStudents() {
		// given teacher A with students S1 and S2, and teacher B with student S3
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID s1 = createUser("STUDENT", "Bella", "bella@example.com", "student-pass-1", true);
		UUID s2 = createUser("STUDENT", "Anna", "anna@example.com", "student-pass-1", true);
		UUID s3 = createUser("STUDENT", "Aaron", "aaron@example.com", "student-pass-1", true);
		assignStudent(teacherA, s1);
		assignStudent(teacherA, s2);
		assignStudent(teacherB, s3);
		TestClient admin = loggedInAdmin();

		// when the admin lists teacher A's students
		var response = admin.get("/api/admin/teachers/" + teacherA + "/students");

		// then the response is 200 and the list contains S1 and S2 only, ordered by name
		response.then().statusCode(200)
				.body("id", contains(s2.toString(), s1.toString()))
				.body("name", contains("Anna", "Bella"))
				.body("email", contains("anna@example.com", "bella@example.com"))
				.body("timeZone", contains("UTC", "UTC"));
	}

	@Test
	@DisplayName("[student-enrolment/R3-S2] Id that is not a teacher is not found")
	void idThatIsNotATeacherIsNotFound() {
		// given a logged-in admin and a student account
		TestClient admin = loggedInAdmin();
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);

		// when the admin lists the students of the student's id, and then of an id with no account
		var ofStudent = admin.get("/api/admin/teachers/" + student + "/students");
		var ofUnknown = admin.get("/api/admin/teachers/" + UUID.randomUUID() + "/students");

		// then both responses are 404 with code NOT_FOUND
		ofStudent.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		ofUnknown.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R3-S3] Admin lists a student's teachers")
	void adminListsAStudentsTeachers() {
		// given a student assigned to teachers A and B, and teacher C without that student
		UUID teacherA = createUser("TEACHER", "Zoe", "a@example.com", "teacher-pass-1", true);
		UUID teacherB = createUser("TEACHER", "Maya", "b@example.com", "teacher-pass-1", true);
		createUser("TEACHER", "Adam", "c@example.com", "teacher-pass-1", true);
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherA, student);
		assignStudent(teacherB, student);
		TestClient admin = loggedInAdmin();

		// when the admin lists the student's teachers
		var response = admin.get("/api/admin/students/" + student + "/teachers");

		// then the response is 200 and the list contains A and B only, ordered by name
		response.then().statusCode(200)
				.body("id", contains(teacherB.toString(), teacherA.toString()))
				.body("name", contains("Maya", "Zoe"))
				.body("email", contains("b@example.com", "a@example.com"))
				.body("timeZone", contains("UTC", "UTC"));
	}

	@Test
	@DisplayName("[student-enrolment/R3-S4] Id that is not a student is not found")
	void idThatIsNotAStudentIsNotFound() {
		// given a logged-in admin and a teacher account
		TestClient admin = loggedInAdmin();
		UUID teacher = createTeacher("teacher@example.com", "teacher-pass-1");

		// when the admin lists the teachers of the teacher's id, and then of an id with no account
		var ofTeacher = admin.get("/api/admin/students/" + teacher + "/teachers");
		var ofUnknown = admin.get("/api/admin/students/" + UUID.randomUUID() + "/teachers");

		// then both responses are 404 with code NOT_FOUND
		ofTeacher.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		ofUnknown.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R3-S5] Malformed id is not found")
	void malformedIdIsNotFound() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin lists the students of the id "not-a-uuid", and then the teachers of the id "not-a-uuid"
		var students = admin.get("/api/admin/teachers/not-a-uuid/students");
		var teachers = admin.get("/api/admin/students/not-a-uuid/teachers");

		// then both responses are 404 with code NOT_FOUND
		students.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		teachers.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R3-S6] Teachers and students cannot call admin endpoints")
	void teachersAndStudentsCannotCallAdminEndpoints() {
		// given teacher A, teacher B with a student, and a logged-in student
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherB, student);
		TestClient teacherA = loggedInAs("a@example.com");
		TestClient loggedInStudent = loggedInAs("student@example.com");

		// when teacher A and the student list teacher B's students and the student's teachers through the admin endpoints
		var teacherStudents = teacherA.get("/api/admin/teachers/" + teacherB + "/students");
		var teacherTeachers = teacherA.get("/api/admin/students/" + student + "/teachers");
		var studentStudents = loggedInStudent.get("/api/admin/teachers/" + teacherB + "/students");
		var studentTeachers = loggedInStudent.get("/api/admin/students/" + student + "/teachers");

		// then all responses are 403
		teacherStudents.then().statusCode(403);
		teacherTeachers.then().statusCode(403);
		studentStudents.then().statusCode(403);
		studentTeachers.then().statusCode(403);
	}

	@Test
	@DisplayName("[student-enrolment/R3-S7] Deactivated teacher is shown as inactive")
	void deactivatedTeacherIsShownAsInactive() {
		// given a student assigned to an active teacher and a deactivated teacher
		UUID active = createUser("TEACHER", "Adam", "active@example.com", "teacher-pass-1", true);
		UUID inactive = createUser("TEACHER", "Zoe", "inactive@example.com", "teacher-pass-1", false);
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(active, student);
		assignStudent(inactive, student);
		TestClient admin = loggedInAdmin();

		// when the admin lists the student's teachers
		var response = admin.get("/api/admin/students/" + student + "/teachers");

		// then both teachers are listed
		response.then().statusCode(200).body("id", contains(active.toString(), inactive.toString()));
		// and the active one has active true and the deactivated one has active false
		response.then()
				.body("find { it.id == '" + active + "' }.active", equalTo(true))
				.body("find { it.id == '" + inactive + "' }.active", equalTo(false));
	}
}
