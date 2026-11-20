package com.teachdash.lessons.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** lessons R5. */
class DeleteLessonTest extends ApiTest {

	private static String lesson(Object courseId, Object lessonId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId;
	}

	private static String lessons(Object courseId) {
		return "/api/teacher/courses/" + courseId + "/lessons";
	}

	private record LessonRow(
			UUID id,
			String title,
			int position,
			Instant updatedAt) {
	}

	private List<LessonRow> storedLessons(UUID courseId) {
		return jdbc.sql("""
				select id, title, position, updated_at
				from lessons
				where course_id = :course
				order by position
				""")
				.param("course", courseId)
				.query((rs, i) -> new LessonRow(
						rs.getObject("id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private boolean isStored(UUID lessonId) {
		return jdbc.sql("select count(*) from lessons where id = :id")
				.param("id", lessonId)
				.query(Long.class)
				.single() > 0;
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")));
	}

	@Test
	@DisplayName("[lessons/R5-S1] Delete a lesson in the middle")
	void deleteALessonInTheMiddle() {
		// given a logged-in teacher A with the course "English B1" with the lessons "One", "Two", "Three" and "Four" at the positions 1 to 4 created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "One", 1);
		UUID two = createLesson(teacherA, course, "Two", 2);
		createLesson(teacherA, course, "Three", 3);
		createLesson(teacherA, course, "Four", 4);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A deletes "Two"
		var response = teacher.delete(lesson(course, two));

		// then the response is 204 with an empty body
		response.then().statusCode(204).body(emptyOrNullString());
		// and "Two" is no longer stored
		assertEquals(false, isStored(two));
		// and the list shows "One", "Three", "Four" at the positions 1, 2, 3, all with updatedAt T
		var listed = teacher.get(lessons(course));
		listed.then().statusCode(200)
				.body("title", equalTo(List.of("One", "Three", "Four")))
				.body("position", equalTo(List.of(1, 2, 3)));
		for (LessonRow row : storedLessons(course)) {
			assertEquals(created, row.updatedAt());
		}
		// and the updatedAt of the course is unchanged
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[lessons/R5-S2] Delete the first Russian lesson")
	void deleteTheFirstRussianLesson() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lessons "Падежи" and "Роды" at the positions 1 and 2
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID cases = createLesson(teacherA, course, "Падежи", 1);
		createLesson(teacherA, course, "Роды", 2);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A deletes "Падежи"
		var response = teacher.delete(lesson(course, cases));

		// then the response is 204
		response.then().statusCode(204);
		// and the course has exactly the lesson "Роды" at position 1
		var stored = storedLessons(course);
		assertEquals(List.of("Роды"), stored.stream().map(LessonRow::title).toList());
		assertEquals(List.of(1), stored.stream().map(LessonRow::position).toList());
	}

	@Test
	@DisplayName("[lessons/R5-S3] A second delete is not found, and a slot is freed")
	void aSecondDeleteIsNotFoundAndASlotIsFreed() {
		// given a logged-in teacher A with a course with 100 lessons
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		List<UUID> ids = new ArrayList<>();
		for (int i = 1; i <= 100; i++) {
			ids.add(createLesson(teacherA, course, "Lesson " + i, i));
		}
		TestClient teacher = loggedInAs("a@example.com");
		UUID middle = ids.get(49);

		// when teacher A deletes the lesson at position 50, deletes it again, and then creates the lesson "Lesson 101" in the course
		var first = teacher.delete(lesson(course, middle));
		var second = teacher.delete(lesson(course, middle));
		var created = teacher.post(lessons(course), java.util.Map.of("title", "Lesson 101"));

		// then the first response is 204
		first.then().statusCode(204);
		// and the second is 404 NOT_FOUND with no errors property
		second.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the third is 201 with position 100
		created.then().statusCode(201).body("position", equalTo(100));
		// and the course has 100 lessons with the positions 1 to 100 without gaps
		assertEquals(100, countLessons(course));
		assertEquals(IntStream.rangeClosed(1, 100).boxed().toList(), lessonPositions(course));
	}

	@Test
	@DisplayName("[lessons/R5-S4] Another teacher's, unknown, malformed and wrong-course ids are not found")
	void anotherTeachersUnknownMalformedAndWrongCourseIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with the lesson "Падежи"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreign, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A deletes teacher B's lesson (under teacher B's course, and under "English B1"), a lesson with an unknown id, the lesson id "not-a-uuid", "Fractions" under "English B1", and "Fractions" under the course "not-a-uuid" and under an unknown course id
		List<Response> responses = List.of(
				teacher.delete(lesson(foreign, foreignLesson)),
				teacher.delete(lesson(english, foreignLesson)),
				teacher.delete(lesson(english, UUID.randomUUID())),
				teacher.delete(lesson(english, "not-a-uuid")),
				teacher.delete(lesson(english, fractions)),
				teacher.delete(lesson("not-a-uuid", fractions)),
				teacher.delete(lesson(UUID.randomUUID(), fractions)));

		// then all responses are 404 NOT_FOUND with no errors property and none contains "Грамматика" or "Падежи"
		responses.forEach(this::assertNotFound);
		// and both lessons are still stored
		assertEquals(true, isStored(foreignLesson));
		assertEquals(true, isStored(fractions));
	}

	@Test
	@DisplayName("[lessons/R5-S5] Only active teachers delete lessons")
	void onlyActiveTeachersDeleteLessons() {
		// given a course of teacher A with a lesson, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Lesson", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each delete the lesson, and teacher B does so without a CSRF token
		String path = lesson(course, lesson);
		var adminResponse = admin.delete(path);
		var studentResponse = student.delete(path);
		var deactivatedResponse = deactivated.delete(path);
		var anonymousResponse = anonymous.delete(path);
		var csrfResponse = withoutCsrf.delete(path);

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
		// and teacher B's is 403 CSRF_INVALID
		csrfResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and the lesson is still stored
		assertEquals(true, isStored(lesson));
	}

	@Test
	@DisplayName("[lessons/R5-S6] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, and a logged-in teacher A who is then deactivated
		TestClient admin = loggedInAdmin();
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();

		// when the admin and teacher A each delete the lesson "not-a-uuid" in the course "not-a-uuid"
		var adminResponse = admin.delete(lesson("not-a-uuid", "not-a-uuid"));
		var deactivatedResponse = deactivated.delete(lesson("not-a-uuid", "not-a-uuid"));

		// then the admin's response is 403 FORBIDDEN and teacher A's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and neither is a 404
		assertEquals(false, adminResponse.statusCode() == 404 || deactivatedResponse.statusCode() == 404);
	}

	@Test
	@DisplayName("[lessons/R5-S7] Parallel deletes and moves keep positions dense")
	void parallelDeletesAndMovesKeepPositionsDense() throws Exception {
		// given a logged-in teacher A with a course with 10 lessons at the positions 1 to 10
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		List<UUID> ids = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			ids.add(createLesson(teacherA, course, "Lesson " + i, i));
		}
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends at the same time 3 delete requests for the lessons at the positions 2, 5 and 8, and 4 move requests for the lessons at the positions 1, 3, 6 and 10, each to position 1
		List<Callable<Response>> calls = new ArrayList<>();
		for (int position : new int[] {2, 5, 8}) {
			TestClient client = clientA.snapshot();
			UUID id = ids.get(position - 1);
			calls.add(() -> client.delete(lesson(course, id)));
		}
		for (int position : new int[] {1, 3, 6, 10}) {
			TestClient client = clientA.snapshot();
			UUID id = ids.get(position - 1);
			calls.add(() -> client.patch(lesson(course, id) + "/position", java.util.Map.of("position", 1)));
		}
		List<Response> responses = Parallel.run(calls);

		// then the 3 delete responses are 204
		for (Response response : responses.subList(0, 3)) {
			response.then().statusCode(204);
		}
		// and the 4 move responses are 200
		for (Response response : responses.subList(3, 7)) {
			response.then().statusCode(200);
		}
		// and the course has exactly 7 lessons with the positions 1 to 7 without gaps and without duplicates
		assertEquals(7, countLessons(course));
		assertEquals(IntStream.rangeClosed(1, 7).boxed().toList(), lessonPositions(course));
	}
}
