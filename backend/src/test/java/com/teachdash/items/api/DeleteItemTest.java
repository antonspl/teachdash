package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.emptyOrNullString;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** items R7. */
class DeleteItemTest extends ItemApiTest {

	private static String itemPath(Object courseId, Object lessonId, Object partId, Object itemId) {
		return items(courseId, lessonId, partId) + "/" + itemId;
	}

	private static String partPath(Object courseId, Object lessonId, Object partId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts/" + partId;
	}

	private static Map<String, Object> position(Object position) {
		Map<String, Object> body = new HashMap<>();
		body.put("position", position);
		return body;
	}

	private record StoredItem(
			UUID id,
			String text,
			int position,
			long version,
			Instant updatedAt) {
	}

	private List<StoredItem> storedItems(UUID partId) {
		return jdbc.sql("""
				select id, payload->>'text' as text, position, version, updated_at
				from items
				where part_id = :part
				order by position
				""")
				.param("part", partId)
				.query((rs, i) -> new StoredItem(
						rs.getObject("id", UUID.class),
						rs.getString("text"),
						rs.getInt("position"),
						rs.getLong("version"),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private List<UUID> createNotes(UUID teacherId, UUID partId, String... texts) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < texts.length; i++) {
			ids.add(createItem(teacherId, partId, "test-note", "{\"text\": \"" + texts[i] + "\"}", i + 1));
		}
		return ids;
	}

	private List<UUID> createNumberedNotes(UUID teacherId, UUID partId, int count) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			ids.add(createItem(teacherId, partId, "test-note", "{\"text\": \"Item " + i + "\"}", i));
		}
		return ids;
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Разминка")))
				.body(not(containsString("Вопрос")));
	}

	@Test
	@DisplayName("[items/R7-S1] Delete a Russian item in the middle")
	void deleteARussianItemInTheMiddle() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the parts "Разминка" and "Итоги", where "Разминка" has four items "Один", "Два", "Три" and "Четыре", all created at a time T with version 1, "Итоги" has four items, a teacher B with a part with four items, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		UUID warmUp = createPart(teacherA, lesson, "Разминка", 1);
		UUID summary = createPart(teacherA, lesson, "Итоги", 2);
		List<UUID> ids = createNotes(teacherA, warmUp, "Один", "Два", "Три", "Четыре");
		createNotes(teacherA, summary, "Первый", "Второй", "Третий", "Четвёртый");
		UUID foreignCourse = createCourse(teacherB, "Grammar", "en", "en");
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Tenses", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Part", 1);
		createNotes(teacherB, foreignPart, "W", "X", "Y", "Z");
		TestClient teacher = loggedInAs("a@example.com");
		var summaryBefore = storedItems(summary);
		var foreignBefore = storedItems(foreignPart);
		testClock().advance(Duration.ofHours(1));

		// when teacher A deletes "Два"
		var response = teacher.delete(itemPath(course, lesson, warmUp, ids.get(1)));

		// then the response is 204 with an empty body
		response.then().statusCode(204).body(emptyOrNullString());
		// and "Два" is no longer stored
		assertEquals(0, jdbc.sql("select count(*) from items where id = :id").param("id", ids.get(1)).query(Long.class).single());
		// and "Разминка" has "Один", "Три", "Четыре" at the positions 1, 2, 3, all with updatedAt T and version 1
		var stored = storedItems(warmUp);
		assertEquals(List.of("Один", "Три", "Четыре"), stored.stream().map(StoredItem::text).toList());
		assertEquals(List.of(1, 2, 3), stored.stream().map(StoredItem::position).toList());
		for (StoredItem item : stored) {
			assertEquals(created, item.updatedAt());
			assertEquals(1, item.version());
		}
		// and the order and updatedAt of the items of "Итоги" and of teacher B's part are unchanged
		assertEquals(summaryBefore, storedItems(summary));
		assertEquals(foreignBefore, storedItems(foreignPart));
		// and the updatedAt of the parts, the lesson and the course is unchanged
		assertEquals(created, readPartUpdatedAt(warmUp));
		assertEquals(created, readPartUpdatedAt(summary));
		assertEquals(created, readLessonUpdatedAt(lesson));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[items/R7-S2] A second delete is not found, and a slot is freed")
	void aSecondDeleteIsNotFoundAndASlotIsFreed() {
		// given a logged-in teacher A with the course "English B1" with a part with 50 items, where the id of the item at position 10 is read in GIVEN, and the item type test-note
		Setup setup = teacherWithPart();
		List<UUID> ids = createNumberedNotes(setup.teacher(), setup.part(), 50);
		UUID tenth = ids.get(9);

		// when teacher A deletes that item by its id, deletes it again by the same id, and then adds an item of the type test-note with the text "Item 51" to the part
		var first = setup.client().delete(itemPath(setup.course(), setup.lesson(), setup.part(), tenth));
		List<Integer> afterFirst = itemPositions(setup.part());
		var second = setup.client().delete(itemPath(setup.course(), setup.lesson(), setup.part(), tenth));
		var third = setup.client().post(setup.items(), note("Item 51"));

		// then the first response is 204
		first.then().statusCode(204);
		// and after it the 49 remaining items are at the positions 1 to 49 before the add
		assertEquals(IntStream.rangeClosed(1, 49).boxed().toList(), afterFirst);
		// and the second is 404 NOT_FOUND with no errors property
		second.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the third is 201 with position 50
		third.then().statusCode(201).body("position", equalTo(50));
		// and the part has 50 items with the positions 1 to 50 without gaps
		assertEquals(IntStream.rangeClosed(1, 50).boxed().toList(), itemPositions(setup.part()));
	}

	@Test
	@DisplayName("[items/R7-S3] Another teacher's, unknown, malformed and misplaced ids are not found")
	void anotherTeachersUnknownMalformedAndMisplacedIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", the items "Half" in "Halves" and "Tenth" in "Tenths", and a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Вопрос"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID quarters = createPart(teacherA, fractions, "Quarters", 2);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		UUID half = createNotes(teacherA, halves, "Half").get(0);
		UUID tenth = createNotes(teacherA, tenths, "Tenth").get(0);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		UUID question = createNotes(teacherB, foreignPart, "Вопрос").get(0);
		TestClient teacher = loggedInAs("a@example.com");
		var halvesBefore = storedItems(halves);
		var tenthsBefore = storedItems(tenths);
		var foreignBefore = storedItems(foreignPart);

		// when teacher A deletes: sixteen requests under the ids listed in the scenario
		List<Response> responses = List.of(
				teacher.delete(itemPath(foreignCourse, foreignLesson, foreignPart, question)),
				teacher.delete(itemPath(maths, fractions, halves, question)),
				teacher.delete(itemPath(maths, fractions, foreignPart, question)),
				teacher.delete(itemPath(maths, foreignLesson, foreignPart, question)),
				teacher.delete(itemPath(foreignCourse, foreignLesson, foreignPart, half)),
				teacher.delete(itemPath(maths, fractions, halves, UUID.randomUUID())),
				teacher.delete(itemPath(maths, fractions, halves, "not-a-uuid")),
				teacher.delete(itemPath(maths, fractions, quarters, half)),
				teacher.delete(itemPath(maths, fractions, halves, tenth)),
				teacher.delete(itemPath(english, fractions, halves, half)),
				teacher.delete(itemPath(maths, fractions, UUID.randomUUID(), half)),
				teacher.delete(itemPath(maths, fractions, "not-a-uuid", half)),
				teacher.delete(itemPath(maths, UUID.randomUUID(), halves, half)),
				teacher.delete(itemPath(maths, "not-a-uuid", halves, half)),
				teacher.delete(itemPath(UUID.randomUUID(), fractions, halves, half)),
				teacher.delete(itemPath("not-a-uuid", fractions, halves, half)));

		// then all responses are 404 NOT_FOUND with no errors property, none contains "Грамматика", "Падежи", "Разминка" or "Вопрос"
		responses.forEach(this::assertNotFound);
		// and all three items are still stored
		assertEquals(halvesBefore, storedItems(halves));
		assertEquals(tenthsBefore, storedItems(tenths));
		assertEquals(foreignBefore, storedItems(foreignPart));
	}

	@Test
	@DisplayName("[items/R7-S4] Only active teachers delete items")
	void onlyActiveTeachersDeleteItems() {
		// given a part of teacher A with an item, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		Setup setup = teacherWithPart();
		UUID item = createNotes(setup.teacher(), setup.part(), "One").get(0);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each delete the item, and teacher B does so without a CSRF token
		String path = itemPath(setup.course(), setup.lesson(), setup.part(), item);
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
		// and the item is still stored
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R7-S5] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, a logged-in teacher A who is then deactivated, and a logged-in teacher B
		TestClient admin = loggedInAdmin();
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin and teacher A each delete the item "not-a-uuid" in the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid", and teacher B does so without a CSRF token
		String path = itemPath("not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid");
		var adminResponse = admin.delete(path);
		var deactivatedResponse = deactivated.delete(path);
		var csrfResponse = withoutCsrf.delete(path);

		// then the admin's response is 403 FORBIDDEN, teacher A's is 401 UNAUTHENTICATED, teacher B's is 403 CSRF_INVALID, and none is a 404
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		csrfResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
	}

	@Test
	@DisplayName("[items/R7-S6] Parallel deletes and moves end with positions dense and no error")
	void parallelDeletesAndMovesEndWithPositionsDenseAndNoError() throws Exception {
		// given a logged-in teacher A with a part with 10 items at the positions 1 to 10, whose ids are read in GIVEN
		Setup setup = teacherWithPart();
		List<UUID> ids = createNumberedNotes(setup.teacher(), setup.part(), 10);

		// when teacher A sends at the same time 3 delete requests for the items that were at the positions 2, 5 and 8, and 4 move requests for the items that were at the positions 1, 3, 6 and 10, each to position 1, all addressed by id
		List<Callable<Response>> calls = new ArrayList<>();
		for (int index : new int[] { 2, 5, 8 }) {
			TestClient client = setup.client().snapshot();
			UUID id = ids.get(index - 1);
			calls.add(() -> client.delete(itemPath(setup.course(), setup.lesson(), setup.part(), id)));
		}
		for (int index : new int[] { 1, 3, 6, 10 }) {
			TestClient client = setup.client().snapshot();
			UUID id = ids.get(index - 1);
			calls.add(() -> client.patch(itemPath(setup.course(), setup.lesson(), setup.part(), id) + "/position", position(1)));
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
		// and the part ends with exactly 7 items with the positions 1 to 7 without gaps and without duplicates
		assertEquals(7, countItems(setup.part()));
		assertEquals(IntStream.rangeClosed(1, 7).boxed().toList(), itemPositions(setup.part()));
		// and the three deleted ids are gone, and the 7 remaining ids are a permutation of the 7 survivors
		Set<UUID> survivors = new java.util.HashSet<>(ids);
		for (int index : new int[] { 2, 5, 8 }) {
			survivors.remove(ids.get(index - 1));
		}
		assertEquals(survivors, Set.copyOf(storedItems(setup.part()).stream().map(StoredItem::id).toList()));
	}

	@Test
	@DisplayName("[items/R7-S7] Parallel deletes and adds end within the limit, with positions dense and no error")
	void parallelDeletesAndAddsEndWithinTheLimit() throws Exception {
		// given a logged-in teacher A with a part with 50 items, whose ids are read in GIVEN, and the item type test-note
		Setup setup = teacherWithPart();
		List<UUID> ids = createNumberedNotes(setup.teacher(), setup.part(), 50);

		// when teacher A sends at the same time 3 delete requests for the items that were at the positions 2, 9 and 15, addressed by id, and 5 add requests of the type test-note with the texts "New 1" to "New 5"
		List<Callable<Response>> calls = new ArrayList<>();
		for (int index : new int[] { 2, 9, 15 }) {
			TestClient client = setup.client().snapshot();
			UUID id = ids.get(index - 1);
			calls.add(() -> client.delete(itemPath(setup.course(), setup.lesson(), setup.part(), id)));
		}
		for (int i = 1; i <= 5; i++) {
			TestClient client = setup.client().snapshot();
			String text = "New " + i;
			calls.add(() -> client.post(setup.items(), note(text)));
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
		// and each add response is 201 or 409 ITEM_LIMIT_REACHED
		int created = 0;
		List<String> createdTexts = new ArrayList<>();
		for (int i = 3; i < 8; i++) {
			Response response = responses.get(i);
			assertTrue(response.statusCode() == 201
					|| (response.statusCode() == 409 && "ITEM_LIMIT_REACHED".equals(response.path("code"))));
			if (response.statusCode() == 201) {
				created++;
				createdTexts.add(response.path("payload.text"));
			}
		}
		// and the final count is 47 plus the number of 201 responses and at most 50
		long count = countItems(setup.part());
		assertEquals(47 + created, count);
		assertTrue(count <= 50);
		// and every text of a 201 response is in the part
		List<String> stored = storedItems(setup.part()).stream().map(StoredItem::text).toList();
		assertTrue(stored.containsAll(createdTexts));
		// and its positions are 1 to the number of its items without gaps and without duplicates
		assertEquals(IntStream.rangeClosed(1, (int) count).boxed().toList(), itemPositions(setup.part()));
	}

	@Test
	@DisplayName("[items/R7-S8] Deleting items while the part is deleted")
	void deletingItemsWhileThePartIsDeleted() throws Exception {
		// given a logged-in teacher A with a part with 5 items, whose ids are read in GIVEN
		Setup setup = teacherWithPart();
		List<UUID> ids = createNumberedNotes(setup.teacher(), setup.part(), 5);

		// when 1 request deleting the part and 4 requests deleting different items of it are sent at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		TestClient partClient = setup.client().snapshot();
		calls.add(() -> partClient.delete(partPath(setup.course(), setup.lesson(), setup.part())));
		for (int i = 0; i < 4; i++) {
			TestClient client = setup.client().snapshot();
			UUID id = ids.get(i);
			calls.add(() -> client.delete(itemPath(setup.course(), setup.lesson(), setup.part(), id)));
		}
		List<Response> responses = Parallel.run(calls);

		// then the part delete response is 204
		responses.get(0).then().statusCode(204);
		// and each item delete response is 204 or 404 and none is 500
		for (Response response : responses.subList(1, 5)) {
			assertTrue(response.statusCode() == 204 || response.statusCode() == 404);
		}
		// and the part is not stored
		assertEquals(0, jdbc.sql("select count(*) from parts where id = :id").param("id", setup.part()).query(Long.class).single());
		// and no item row references it
		assertEquals(0, countItems(setup.part()));
	}
}
