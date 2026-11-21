package com.teachdash.parts.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** parts R5. */
class DeletePartTest extends ApiTest {

	private static String part(Object courseId, Object lessonId, Object partId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts/" + partId;
	}

	private static String parts(Object courseId, Object lessonId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts";
	}

	private static Map<String, Object> title(Object title) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		return body;
	}

	private static Map<String, Object> position(Object position) {
		Map<String, Object> body = new HashMap<>();
		body.put("position", position);
		return body;
	}

	private record PartRow(
			UUID id,
			String title,
			int position,
			Instant updatedAt) {
	}

	private List<PartRow> storedParts(UUID lessonId) {
		return jdbc.sql("""
				select id, title, position, updated_at
				from parts
				where lesson_id = :lesson
				order by position
				""")
				.param("lesson", lessonId)
				.query((rs, i) -> new PartRow(
						rs.getObject("id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private List<UUID> createParts(UUID teacherId, UUID lessonId, String... titles) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < titles.length; i++) {
			ids.add(createPart(teacherId, lessonId, titles[i], i + 1));
		}
		return ids;
	}

	private List<UUID> createNumberedParts(UUID teacherId, UUID lessonId, int count) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			ids.add(createPart(teacherId, lessonId, "Part " + i, i));
		}
		return ids;
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Разминка")));
	}

	@Test
	@DisplayName("[parts/R5-S1] Delete a Russian part in the middle")
	void deleteARussianPartInTheMiddle() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lessons "Падежи" and "Роды", each with four parts at the positions 1 to 4, where "Падежи" has the parts "Один", "Два", "Три" and "Четыре", a teacher B with a lesson with four parts at the positions 1 to 4, all created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID cases = createLesson(teacherA, course, "Падежи", 1);
		UUID genders = createLesson(teacherA, course, "Роды", 2);
		List<UUID> casesParts = createParts(teacherA, cases, "Один", "Два", "Три", "Четыре");
		createParts(teacherA, genders, "Первый", "Второй", "Третий", "Четвёртый");
		UUID foreignCourse = createCourse(teacherB, "Grammar", "en", "en");
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Tenses", 1);
		createParts(teacherB, foreignLesson, "W", "X", "Y", "Z");
		TestClient teacher = loggedInAs("a@example.com");
		var gendersBefore = storedParts(genders);
		var foreignBefore = storedParts(foreignLesson);
		testClock().advance(Duration.ofHours(1));

		// when teacher A deletes "Два"
		var response = teacher.delete(part(course, cases, casesParts.get(1)));

		// then the response is 204 with an empty body
		response.then().statusCode(204);
		assertEquals("", response.asString());
		// and "Два" is no longer stored
		assertEquals(0, jdbc.sql("select count(*) from parts where id = :id")
				.param("id", casesParts.get(1))
				.query(Long.class)
				.single());
		// and the list of "Падежи" shows "Один", "Три", "Четыре" at the positions 1, 2, 3, all with updatedAt T
		var stored = storedParts(cases);
		assertEquals(List.of("Один", "Три", "Четыре"), stored.stream().map(PartRow::title).toList());
		assertEquals(List.of(1, 2, 3), stored.stream().map(PartRow::position).toList());
		stored.forEach(row -> assertEquals(created, row.updatedAt()));
		// and the order and updatedAt of the parts of "Роды" and of teacher B's lesson are unchanged
		assertEquals(gendersBefore, storedParts(genders));
		assertEquals(foreignBefore, storedParts(foreignLesson));
		// and the updatedAt of the lessons and of the course is unchanged
		assertEquals(created, readLessonUpdatedAt(cases));
		assertEquals(created, readLessonUpdatedAt(genders));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[parts/R5-S2] A second delete is not found, and a slot is freed")
	void aSecondDeleteIsNotFoundAndASlotIsFreed() {
		// given a logged-in teacher A with the course "English B1" with a lesson with 20 parts, where the id of the part at position 10 is read in GIVEN
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		List<UUID> parts = createNumberedParts(teacherA, lesson, 20);
		UUID tenth = parts.get(9);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A deletes that part by its id, deletes it again by the same id, and then creates the part "Part 21" in the lesson
		var first = teacher.delete(part(course, lesson, tenth));
		var second = teacher.delete(part(course, lesson, tenth));
		var third = teacher.post(parts(course, lesson), title("Part 21"));

		// then the first response is 204
		first.then().statusCode(204);
		// and the second is 404 NOT_FOUND with no errors property
		second.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the third is 201 with position 20
		third.then().statusCode(201).body("position", equalTo(20));
		// and the lesson has 20 parts with the positions 1 to 20 without gaps
		assertEquals(20, countParts(lesson));
		assertEquals(IntStream.rangeClosed(1, 20).boxed().toList(), partPositions(lesson));
	}

	@Test
	@DisplayName("[parts/R5-S3] Another teacher's, unknown, malformed and misplaced ids are not found")
	void anotherTeachersUnknownMalformedAndMisplacedIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");
		var fractionsBefore = storedParts(fractions);
		var decimalsBefore = storedParts(decimals);
		var foreignBefore = storedParts(foreignLesson);

		// when teacher A deletes each: twelve parts under the ids listed in the scenario
		List<Response> responses = List.of(
				teacher.delete(part(foreignCourse, foreignLesson, foreignPart)),
				teacher.delete(part(english, foreignLesson, foreignPart)),
				teacher.delete(part(maths, fractions, foreignPart)),
				teacher.delete(part(foreignCourse, foreignLesson, halves)),
				teacher.delete(part(maths, fractions, UUID.randomUUID())),
				teacher.delete(part(maths, fractions, "not-a-uuid")),
				teacher.delete(part(maths, fractions, tenths)),
				teacher.delete(part(english, fractions, halves)),
				teacher.delete(part(maths, "not-a-uuid", halves)),
				teacher.delete(part(maths, UUID.randomUUID(), halves)),
				teacher.delete(part("not-a-uuid", fractions, halves)),
				teacher.delete(part(UUID.randomUUID(), fractions, halves)));

		// then all responses are 404 NOT_FOUND with no errors property, none contains "Грамматика", "Падежи" or "Разминка"
		responses.forEach(this::assertNotFound);
		// and all three parts are still stored
		assertEquals(fractionsBefore, storedParts(fractions));
		assertEquals(decimalsBefore, storedParts(decimals));
		assertEquals(foreignBefore, storedParts(foreignLesson));
	}

	@Test
	@DisplayName("[parts/R5-S4] Only active teachers delete parts")
	void onlyActiveTeachersDeleteParts() {
		// given a lesson of teacher A with a part, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "One", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each delete the part, and teacher B does so without a CSRF token
		String path = part(course, lesson, part);
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
		// and the part is still stored
		assertEquals(1, countParts(lesson));
	}

	@Test
	@DisplayName("[parts/R5-S5] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, and a logged-in teacher A who is then deactivated
		TestClient admin = loggedInAdmin();
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();

		// when the admin and teacher A each delete the part "not-a-uuid" in the lesson "not-a-uuid" of the course "not-a-uuid"
		var adminResponse = admin.delete(part("not-a-uuid", "not-a-uuid", "not-a-uuid"));
		var deactivatedResponse = deactivated.delete(part("not-a-uuid", "not-a-uuid", "not-a-uuid"));

		// then the admin's response is 403 FORBIDDEN and teacher A's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}

	@Test
	@DisplayName("[parts/R5-S6] Parallel deletes and moves end with positions dense and no error")
	void parallelDeletesAndMovesEndWithPositionsDenseAndNoError() throws Exception {
		// given a logged-in teacher A with a lesson with 10 parts at the positions 1 to 10, whose ids are read in GIVEN
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		List<UUID> parts = createNumberedParts(teacherA, lesson, 10);
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends at the same time 3 delete requests for the parts that were at the positions 2, 5 and 8, and 4 move requests for the parts that were at the positions 1, 3, 6 and 10, each to position 1, all addressed by id
		List<Callable<Response>> calls = new ArrayList<>();
		for (int index : new int[] { 2, 5, 8 }) {
			TestClient client = clientA.snapshot();
			UUID id = parts.get(index - 1);
			calls.add(() -> client.delete(part(course, lesson, id)));
		}
		for (int index : new int[] { 1, 3, 6, 10 }) {
			TestClient client = clientA.snapshot();
			UUID id = parts.get(index - 1);
			calls.add(() -> client.patch(part(course, lesson, id) + "/position", position(1)));
		}
		List<Response> responses = Parallel.run(calls);

		// then none of the responses is 500
		for (Response response : responses) {
			assertTrue(response.statusCode() != 500);
		}
		// and the delete responses are 204
		for (Response response : responses.subList(0, 3)) {
			response.then().statusCode(204);
		}
		// and the move responses are 200
		for (Response response : responses.subList(3, 7)) {
			response.then().statusCode(200);
		}
		// and the lesson ends with exactly 7 parts with the positions 1 to 7 without gaps and without duplicates
		assertEquals(7, countParts(lesson));
		assertEquals(IntStream.rangeClosed(1, 7).boxed().toList(), partPositions(lesson));
	}

	@Test
	@DisplayName("[parts/R5-S7] Parallel deletes and creates end within the limit, with positions dense and no error")
	void parallelDeletesAndCreatesEndWithinTheLimitWithPositionsDenseAndNoError() throws Exception {
		// given a logged-in teacher A with a lesson with 20 parts, whose ids are read in GIVEN
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		List<UUID> parts = createNumberedParts(teacherA, lesson, 20);
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends at the same time 3 delete requests for the parts that were at the positions 2, 9 and 15, addressed by id, and 5 create requests with the titles "New 1" to "New 5"
		List<Callable<Response>> calls = new ArrayList<>();
		for (int index : new int[] { 2, 9, 15 }) {
			TestClient client = clientA.snapshot();
			UUID id = parts.get(index - 1);
			calls.add(() -> client.delete(part(course, lesson, id)));
		}
		for (int i = 1; i <= 5; i++) {
			TestClient client = clientA.snapshot();
			String title = "New " + i;
			calls.add(() -> client.post(parts(course, lesson), title(title)));
		}
		List<Response> responses = Parallel.run(calls);

		// then none of the responses is 500
		for (Response response : responses) {
			assertTrue(response.statusCode() != 500);
		}
		// and the delete responses are 204
		for (Response response : responses.subList(0, 3)) {
			response.then().statusCode(204);
		}
		// and each create response is 201 or 409 PART_LIMIT_REACHED
		for (Response response : responses.subList(3, 8)) {
			assertTrue(response.statusCode() == 201
					|| (response.statusCode() == 409 && "PART_LIMIT_REACHED".equals(response.path("code"))));
		}
		// and the lesson ends with at most 20 parts
		long count = countParts(lesson);
		assertTrue(count <= 20);
		// and its positions are 1 to the number of its parts without gaps and without duplicates
		assertEquals(IntStream.rangeClosed(1, (int) count).boxed().toList(), partPositions(lesson));
	}
}
