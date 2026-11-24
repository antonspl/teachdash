package com.teachdash.items.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.TestClient;

/** items R3. */
class ListItemsTest extends ItemApiTest {

	@Test
	@DisplayName("[items/R3-S1] Items are listed by position with the item fields")
	void itemsAreListedByPositionWithTheItemFields() {
		// given a logged-in teacher A with a part with three test-note items inserted in the order 3, 1, 2 at a time T, the last with version 3
		var setup = teacherWithPart();
		Instant created = Instant.now(clock);
		String third = "Прочитайте текст вслух: ёж & ёлка.";
		createItem(setup.teacher(), setup.part(), "test-note", "Немного теории", "{\"text\": \"" + third + "\"}", 3, 3);
		createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Hi\"}", 1);
		createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Read the story aloud.\"}", 2, "Reading aloud");

		// when teacher A lists the items of the part
		var response = setup.client().get(setup.items());

		// then the response is 200 with the texts in the order of the positions
		response.then().statusCode(200)
				.body("$", hasSize(3))
				.body("payload.text", equalTo(List.of("Hi", "Read the story aloud.", third)))
				.body("position", equalTo(List.of(1, 2, 3)))
				.body("type", equalTo(List.of("test-note", "test-note", "test-note")))
				.body("title", equalTo(Arrays.asList(null, "Reading aloud", "Немного теории")))
				.body("version", equalTo(List.of(1, 1, 3)));
		// and every element has exactly the item properties
		List<Map<String, Object>> elements = response.path("$");
		for (Map<String, Object> element : elements) {
			assertEquals(Set.of("id", "type", "title", "payload", "position", "version", "createdAt", "updatedAt"),
					element.keySet());
			// and updatedAt equals createdAt
			assertEquals(element.get("createdAt"), element.get("updatedAt"));
			assertEquals(created, Instant.parse((String) element.get("createdAt")));
		}
		// and every payload is exactly the stored payload
		for (Map<String, Object> element : elements) {
			assertStoredPayload(UUID.fromString((String) element.get("id")), toJson(element.get("payload")));
		}
	}

	private static String toJson(Object payload) {
		return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(payload);
	}

	@Test
	@DisplayName("[items/R3-S2] A part without items gives an empty list, and only that part's items are listed")
	void aPartWithoutItemsGivesAnEmptyList() {
		// given a logged-in teacher A with the lesson "Падежи" with the part "Пусто" with no items, the part "Разминка" with the item "Один", and a teacher B with a part with the item "Один"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Грамматика", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		UUID empty = createPart(teacherA, lesson, "Пусто", 1);
		UUID warmUp = createPart(teacherA, lesson, "Разминка", 2);
		UUID item = createItem(teacherA, warmUp, "test-note", "{\"text\": \"Один\"}", 1);
		UUID otherCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID otherLesson = createLesson(teacherB, otherCourse, "Падежи", 1);
		UUID otherPart = createPart(teacherB, otherLesson, "Разминка", 1);
		createItem(teacherB, otherPart, "test-note", "{\"text\": \"Один\"}", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the items of "Пусто", then of "Разминка"
		var first = teacher.get(items(course, lesson, empty));
		var second = teacher.get(items(course, lesson, warmUp));

		// then the first response is 200 with an empty array
		first.then().statusCode(200).body("$", hasSize(0));
		// and the second is 200 with exactly one element, the item of "Разминка"
		second.then().statusCode(200)
				.body("$", hasSize(1))
				.body("[0].id", equalTo(item.toString()))
				.body("[0].payload.text", equalTo("Один"));
	}

	@Test
	@DisplayName("[items/R3-S3] Another teacher's, unknown, malformed and wrong-parent ids are not found")
	void anotherTeachersUnknownMalformedAndWrongParentIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" (with an item) and "Tenths", and a course of teacher B with a lesson, a part and an item "Секрет"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		createItem(teacherA, halves, "test-note", "{\"text\": \"Half\"}", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		createItem(teacherB, foreignPart, "test-note", "{\"text\": \"Секрет\"}", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the items of the twelve wrong combinations of ids
		List<String> paths = List.of(
				items(foreignCourse, foreignLesson, foreignPart),
				items(maths, fractions, foreignPart),
				items(english, foreignLesson, foreignPart),
				items(foreignCourse, foreignLesson, halves),
				items(maths, fractions, UUID.randomUUID()),
				items(maths, fractions, "not-a-uuid"),
				items(maths, UUID.randomUUID(), halves),
				items(maths, "not-a-uuid", halves),
				items(maths, fractions, tenths),
				items(english, fractions, halves),
				items(UUID.randomUUID(), fractions, halves),
				items("not-a-uuid", fractions, halves));

		// then all responses are 404 NOT_FOUND with no errors property and no text of teacher B
		for (String path : paths) {
			expectNotFound(teacher.get(path));
		}
	}

	@Test
	@DisplayName("[items/R3-S4] Only active teachers list items")
	void onlyActiveTeachersListItems() {
		// given a part of teacher A with an item, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Warm-up", 1);
		createItem(teacherA, part, "test-note", "{\"text\": \"Hi\"}", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");

		// when each of them lists the items of the part
		String path = items(course, lesson, part);
		var adminResponse = admin.get(path);
		var studentResponse = student.get(path);
		var deactivatedResponse = deactivated.get(path);
		var anonymousResponse = anonymous.get(path);

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
	}

	@Test
	@DisplayName("[items/R3-S5] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, and a logged-in teacher A2 who is then deactivated
		TestClient admin = loggedInAdmin();
		createTeacher("a2@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a2@example.com");
		jdbc.sql("update users set active = false where email = 'a2@example.com'").update();

		// when the admin and teacher A2 each list the items of the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid"
		String path = items("not-a-uuid", "not-a-uuid", "not-a-uuid");
		var adminResponse = admin.get(path);
		var deactivatedResponse = deactivated.get(path);

		// then the admin's response is 403 FORBIDDEN and teacher A2's is 401 UNAUTHENTICATED, neither is a 404
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}
}
