package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** items R2. */
class ItemPayloadTest extends ItemApiTest {

	private static final Map<String, Object> HI = Map.of("text", "Hi");

	private static String raw(String type, String payloadJson) {
		return "{\"type\":\"" + type + "\",\"payload\":" + payloadJson + "}";
	}

	/** Padding of exactly the given number of UTF-8 bytes: "Я" is 2 bytes, one ASCII letter fixes an odd count. */
	private static String padding(int bytes) {
		return (bytes % 2 == 1 ? "a" : "") + "Я".repeat(bytes / 2);
	}

	private void assertValidation(Response response, Map<String, String>... expected) {
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(expected)));
	}

	@Test
	@DisplayName("[items/R2-S1] A missing, null, blank, wrongly typed or unknown type is rejected")
	void aMissingNullBlankWronglyTypedOrUnknownTypeIsRejected() {
		// given a logged-in teacher A with a part, and the item type test-note (no type poll is registered)
		var setup = teacherWithPart();

		// when teacher A adds items with no type, a null type, the types "" and "  ", the type 123, the type "poll", the body {}, and the type "poll" with the payload []
		var missing = setup.client().post(setup.items(), Map.of("payload", HI));
		var nullType = setup.client().post(setup.items(), body(null, HI));
		var empty = setup.client().post(setup.items(), body("", HI));
		var blank = setup.client().post(setup.items(), body("  ", HI));
		var number = setup.client().post(setup.items(), body(123, HI));
		var unknown = setup.client().post(setup.items(), body("poll", HI));
		var noFields = setup.client().post(setup.items(), Map.of());
		var unknownAndList = setup.client().post(setup.items(), body("poll", List.of()));

		// then all responses are 400 VALIDATION_FAILED with the errors of the scenario
		for (Response response : List.of(missing, nullType, empty, blank)) {
			assertValidation(response, error("type", "REQUIRED"));
		}
		assertValidation(number, error("type", "INVALID_TYPE"));
		assertValidation(unknown, error("type", "UNKNOWN_TYPE"));
		assertValidation(noFields, error("payload", "REQUIRED"), error("type", "REQUIRED"));
		assertValidation(unknownAndList, error("payload", "INVALID_TYPE"), error("type", "UNKNOWN_TYPE"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S2] A missing or null payload is rejected, a broken body is malformed")
	void aMissingOrNullPayloadIsRejectedABrokenBodyIsMalformed() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the bodies {"type": "test-note"}, {"type": "test-note", "payload": null}, {"type": "test-note", "payload": and no body
		var missing = setup.client().post(setup.items(), Map.of("type", "test-note"));
		var nullPayload = setup.client().post(setup.items(), body("test-note", null));
		var truncated = setup.client().post(setup.items(), "{\"type\": \"test-note\", \"payload\":");
		var noBody = setup.client().post(setup.items());

		// then the first two responses are 400 VALIDATION_FAILED with payload REQUIRED
		for (Response response : List.of(missing, nullPayload)) {
			assertValidation(response, error("payload", "REQUIRED"));
		}
		// and the last two are 400 MALFORMED_REQUEST with no errors property
		for (Response response : List.of(truncated, noBody)) {
			response.then().statusCode(400)
					.body("code", equalTo("MALFORMED_REQUEST"))
					.body("$", not(hasKey("errors")));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S3] A payload that is not an object is rejected before the type's rules run")
	void aPayloadThatIsNotAnObjectIsRejectedBeforeTheTypesRulesRun() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with the payload [], ["Hi"], "Hi", 123 and true
		List<Response> responses = List.of(
				setup.client().post(setup.items(), body("test-note", List.of())),
				setup.client().post(setup.items(), body("test-note", List.of("Hi"))),
				setup.client().post(setup.items(), body("test-note", "Hi")),
				setup.client().post(setup.items(), body("test-note", 123)),
				setup.client().post(setup.items(), body("test-note", true)));

		// then every response is 400 VALIDATION_FAILED with payload INVALID_TYPE only
		for (Response response : responses) {
			assertValidation(response, error("payload", "INVALID_TYPE"));
		}
		// and no body contains "Hi" as a value
		for (Response response : responses) {
			response.then().body(not(containsString("\"Hi\"")));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S4] The payload size limit is 64 KiB of UTF-8 bytes")
	void thePayloadSizeLimitIs64KiBOfUtf8Bytes() {
		// given a logged-in teacher A with a part, a payload A of exactly 65536 bytes, a payload B of 65537 bytes, and a payload C of 65537 bytes with no text
		var setup = teacherWithPart();
		String hintA = padding(65536 - "{\"text\":\"Я\",\"hint\":\"\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
		String payloadA = "{\"text\":\"Я\",\"hint\":\"" + hintA + "\"}";
		String payloadB = "{\"text\":\"Я\",\"hint\":\"" + hintA + "a\"}";
		String hintC = padding(65537 - "{\"hint\":\"\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
		String payloadC = "{\"hint\":\"" + hintC + "\"}";

		// when teacher A adds an item of the type test-note with payload A, then B, then C
		var a = setup.client().post(setup.items(), raw("test-note", payloadA));
		var b = setup.client().post(setup.items(), raw("test-note", payloadB));
		var c = setup.client().post(setup.items(), raw("test-note", payloadC));

		// then the first response is 201 with the stored hint equal to the sent one
		a.then().statusCode(201).body("payload.hint", equalTo(hintA));
		String storedHint = jdbc.sql("select payload->>'hint' from items where part_id = :part")
				.param("part", setup.part())
				.query(String.class)
				.single();
		assertEquals(hintA, storedHint);
		// and the second and the third are 400 VALIDATION_FAILED with payload TOO_LONG only
		assertValidation(b, error("payload", "TOO_LONG"));
		assertValidation(c, error("payload", "TOO_LONG"));
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S5] A NUL character in a string value is rejected at any depth")
	void aNulCharacterInAStringValueIsRejectedAtAnyDepth() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with a NUL in text, in hint and deep in extra
		List<Response> responses = List.of(
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"a\\u0000b\"}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"hint\": \"a\\u0000\"}")),
				setup.client().post(setup.items(),
						raw("test-note", "{\"text\": \"ok\", \"extra\": {\"list\": [\"fine\", {\"deep\": \"x\\u0000y\"}]}}")));

		// then every response is 400 VALIDATION_FAILED with payload INVALID_FORMAT only
		for (Response response : responses) {
			assertValidation(response, error("payload", "INVALID_FORMAT"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S6] A NUL character in an object key is rejected")
	void aNulCharacterInAnObjectKeyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with a NUL in a top-level key and in a nested key
		List<Response> responses = List.of(
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"ke\\u0000y\": 1}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"extra\": {\"\\u0000\": true}}")));

		// then both responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT only
		for (Response response : responses) {
			assertValidation(response, error("payload", "INVALID_FORMAT"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S7] The type's validator reports errors under payload.")
	void theTypesValidatorReportsErrorsUnderPayload() {
		// given a logged-in teacher A with a part, and the type test-note whose text is required, a string of at most 1000 characters
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with the payloads {}, {"text": null}, {"text": 42}, {"text": ["Привет"]} and a text of 1001 Cyrillic letters
		Map<String, Object> nullText = new HashMap<>();
		nullText.put("text", null);
		List<Response> responses = List.of(
				setup.client().post(setup.items(), body("test-note", Map.of())),
				setup.client().post(setup.items(), body("test-note", nullText)),
				setup.client().post(setup.items(), body("test-note", Map.of("text", 42))),
				setup.client().post(setup.items(), body("test-note", Map.of("text", List.of("Привет")))),
				setup.client().post(setup.items(), note("Я".repeat(1001))));

		// then the responses are 400 VALIDATION_FAILED with REQUIRED, REQUIRED, INVALID_TYPE, INVALID_TYPE and TOO_LONG for payload.text
		assertValidation(responses.get(0), error("payload.text", "REQUIRED"));
		assertValidation(responses.get(1), error("payload.text", "REQUIRED"));
		assertValidation(responses.get(2), error("payload.text", "INVALID_TYPE"));
		assertValidation(responses.get(3), error("payload.text", "INVALID_TYPE"));
		assertValidation(responses.get(4), error("payload.text", "TOO_LONG"));
		// and no body contains a rejectedValue or "Привет"
		for (Response response : responses) {
			response.then().body(not(containsString("rejectedValue"))).body(not(containsString("Привет")));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S8] A payload at the type's limit is accepted in both languages")
	void aPayloadAtTheTypesLimitIsAcceptedInBothLanguages() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type test-note with a text of 1000 Cyrillic letters, then one with 1000 English letters
		String cyrillic = "Я".repeat(1000);
		String english = "a".repeat(1000);
		var first = setup.client().post(setup.items(), note(cyrillic));
		var second = setup.client().post(setup.items(), note(english));

		// then both responses are 201 with the text exactly as sent and position 1 and 2
		first.then().statusCode(201).body("payload.text", equalTo(cyrillic)).body("position", equalTo(1));
		second.then().statusCode(201).body("payload.text", equalTo(english)).body("position", equalTo(2));
	}

	@Test
	@DisplayName("[items/R2-S9] The stored payload is the normalised jsonb form, with the type's schema version")
	void theStoredPayloadIsTheNormalisedJsonbFormWithTheTypesSchemaVersion() {
		// given a logged-in teacher A with a part, and the type test-note with the schema version 2
		var setup = teacherWithPart();

		// when teacher A adds an item with extra spaces and the key text twice
		String body = "{\"type\": \"test-note\", \"payload\": {\"hint\":  \"Ответ: «мышь»\", \"text\":  \"Привет\", \"text\": \"Мир\"}}";
		var response = setup.client().post(setup.items(), body);

		// then the response is 201 with the payload equal to {"text": "Мир", "hint": "Ответ: «мышь»"} and version 1
		response.then().statusCode(201)
				.body("payload", equalTo(Map.of("text", "Мир", "hint", "Ответ: «мышь»")))
				.body("version", equalTo(1));
		// and the stored row has the same payload, the type test-note and schema_version 2
		UUID id = UUID.fromString(response.path("id"));
		var stored = readItem(id);
		assertEquals("test-note", stored.type());
		assertEquals(2, stored.schemaVersion());
		boolean samePayload = jdbc.sql("select payload = cast(:expected as jsonb) from items where id = :id")
				.param("expected", "{\"text\": \"Мир\", \"hint\": \"Ответ: «мышь»\"}")
				.param("id", id)
				.query(Boolean.class)
				.single();
		assertTrue(samePayload);
	}
}
