package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import io.restassured.response.Response;
import tools.jackson.databind.json.JsonMapper;

/** Shared helpers of the item API tests. */
abstract class ItemApiTest extends ApiTest {

	/** Teacher A, logged in, with a course, a lesson and a part. */
	protected record Setup(
			UUID teacher,
			UUID course,
			UUID lesson,
			UUID part,
			TestClient client) {

		String items() {
			return ItemApiTest.items(course, lesson, part);
		}
	}

	protected Setup teacherWithPart() {
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "English B1", "en", "en");
		UUID lesson = createLesson(teacher, course, "Present Simple", 1);
		UUID part = createPart(teacher, lesson, "Warm-up", 1);
		return new Setup(teacher, course, lesson, part, loggedInAs("a@example.com"));
	}

	protected static String items(Object courseId, Object lessonId, Object partId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts/" + partId + "/items";
	}

	protected static Map<String, Object> body(Object type, Object payload) {
		Map<String, Object> body = new HashMap<>();
		body.put("type", type);
		body.put("payload", payload);
		return body;
	}

	protected static Map<String, Object> note(String text) {
		return body("test-note", Map.of("text", text));
	}

	protected static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	protected String storedText(UUID partId, int position) {
		return jdbc.sql("select payload->>'text' from items where part_id = :part and position = :position")
				.param("part", partId)
				.param("position", position)
				.query(String.class)
				.single();
	}

	private static final JsonMapper JSON = JsonMapper.builder().build();

	protected static String item(Object courseId, Object lessonId, Object partId, Object itemId) {
		return items(courseId, lessonId, partId) + "/" + itemId;
	}

	/** A raw edit body, so that any JSON (a NUL escape, a long number) reaches the server as written. */
	protected static String editBody(Object version, String payloadJson) {
		return "{\"version\":" + version + ",\"payload\":" + payloadJson + "}";
	}

	protected static String rawBody(String type, String payloadJson) {
		return "{\"type\":\"" + type + "\",\"payload\":" + payloadJson + "}";
	}

	/** The stored payload equals the JSON, ignoring the formatting jsonb adds. */
	protected void assertStoredPayload(UUID itemId, String expectedJson) {
		assertEquals(JSON.readTree(expectedJson), JSON.readTree(readItem(itemId).payload()));
	}

	@SafeVarargs
	protected static void expectValidation(Response response, Map<String, String>... expected) {
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(expected)));
	}

	protected static void expectNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Разминка")))
				.body(not(containsString("Секрет")));
	}
}
