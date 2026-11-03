package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

class StudentListTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";

	@Test
	@DisplayName("[student-enrolment/R2-S1] List shows only own students")
	void listShowsOnlyOwnStudents() {
		// given teacher A with students S1 and S2, and teacher B with student S3
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID s1 = createUser("STUDENT", "Bella", "bella@example.com", "student-pass-1", true);
		UUID s2 = createUser("STUDENT", "Anna", "anna@example.com", "student-pass-1", true);
		UUID s3 = createUser("STUDENT", "Aaron", "aaron@example.com", "student-pass-1", true);
		assignStudent(teacherA, s1);
		assignStudent(teacherA, s2);
		assignStudent(teacherB, s3);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists students
		var response = teacher.get(STUDENTS);

		// then the list contains S1 and S2 only, ordered by name, each with id, name, email and time zone
		response.then().statusCode(200)
				.body("id", contains(s2.toString(), s1.toString()))
				.body("name", contains("Anna", "Bella"))
				.body("email", contains("anna@example.com", "bella@example.com"))
				.body("timeZone", contains("UTC", "UTC"));
	}

	@Test
	@DisplayName("[student-enrolment/R2-S2] Another teacher's student is not found")
	void anotherTeachersStudentIsNotFound() {
		// given a student assigned only to teacher B
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherB, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A requests that student's details, and then the details of an id with no account
		var foreign = teacher.get(STUDENTS + "/" + student);
		var unknown = teacher.get(STUDENTS + "/" + UUID.randomUUID());

		// then both responses are 404 with code NOT_FOUND
		foreign.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		unknown.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R2-S3] Student cannot call teacher endpoints")
	void studentCannotCallTeacherEndpoints() {
		// given a logged-in student
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");

		// when the student calls the teacher endpoint that lists students
		var response = student.get(STUDENTS);

		// then the response is 403
		response.then().statusCode(403);
	}

	@Test
	@DisplayName("[student-enrolment/R2-S4] Teacher reads own student's details")
	void teacherReadsOwnStudentsDetails() {
		// given a student assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A requests that student's details
		var response = teacher.get(STUDENTS + "/" + student);

		// then the response is 200 with the student's id, name, email and time zone
		response.then().statusCode(200)
				.body("id", equalTo(student.toString()))
				.body("name", equalTo("Student"))
				.body("email", equalTo("student@example.com"))
				.body("timeZone", equalTo("UTC"));
	}

	@Test
	@DisplayName("[student-enrolment/R2-S5] Admin cannot call teacher endpoints")
	void adminCannotCallTeacherEndpoints() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin calls the teacher endpoint that lists students
		var response = admin.get(STUDENTS);

		// then the response is 403
		response.then().statusCode(403);
	}
}
