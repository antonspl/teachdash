package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** items R2. */
class ItemPayloadTest extends ItemApiTest {

	private static final Map<String, Object> HI = Map.of("text", "Hi");

	private static final JsonMapper EXACT = JsonMapper.builder()
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.build();

	private static String raw(String type, String payloadJson) {
		return "{\"type\":\"" + type + "\",\"payload\":" + payloadJson + "}";
	}

	@SafeVarargs
	private static void assertValidation(Response response, Map<String, String>... expected) {
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(expected)));
	}

	@Test
	@DisplayName("[items/R2-S1] A missing, null, blank, wrongly typed or unknown type is rejected")
	void aMissingNullBlankWronglyTypedOrUnknownTypeIsRejected() {
		// given a logged-in teacher A with a part, and the item type test-note (no type poll is registered)
		var setup = teacherWithPart();

		// when teacher A adds items with no type, a null type, the types "" and "  ", the type 123, the type "poll", and the body {}
		var missing = setup.client().post(setup.items(), Map.of("payload", HI));
		var nullType = setup.client().post(setup.items(), body(null, HI));
		var empty = setup.client().post(setup.items(), body("", HI));
		var blank = setup.client().post(setup.items(), body("  ", HI));
		var number = setup.client().post(setup.items(), body(123, HI));
		var unknown = setup.client().post(setup.items(), body("poll", HI));
		var noFields = setup.client().post(setup.items(), Map.of());

		// then all responses are 400 VALIDATION_FAILED with the errors of the scenario
		for (Response response : List.of(missing, nullType, empty, blank)) {
			assertValidation(response, error("type", "REQUIRED"));
		}
		assertValidation(number, error("type", "INVALID_TYPE"));
		assertValidation(unknown, error("type", "UNKNOWN_TYPE"));
		assertValidation(noFields, error("payload", "REQUIRED"), error("type", "REQUIRED"));
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
	@DisplayName("[items/R2-S3] A payload that is not an object is rejected before the type is checked, the first binding error wins")
	void aPayloadThatIsNotAnObjectIsRejectedBeforeTheTypeIsChecked() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with the payload [], ["Hi"], "Hi", "", 123 and true, an item of the type "poll" with the payload [], one with a blank type and the payload [], and the body {"type":123,"payload":[]}
		List<Response> notObject = List.of(
				setup.client().post(setup.items(), body("test-note", List.of())),
				setup.client().post(setup.items(), body("test-note", List.of("Hi"))),
				setup.client().post(setup.items(), body("test-note", "Hi")),
				setup.client().post(setup.items(), body("test-note", "")),
				setup.client().post(setup.items(), body("test-note", 123)),
				setup.client().post(setup.items(), body("test-note", true)),
				setup.client().post(setup.items(), body("poll", List.of())),
				setup.client().post(setup.items(), body("  ", List.of())));
		var typeFirst = setup.client().post(setup.items(), "{\"type\":123,\"payload\":[]}");

		// then every response is 400 VALIDATION_FAILED; all but the last have payload INVALID_TYPE only
		for (Response response : notObject) {
			assertValidation(response, error("payload", "INVALID_TYPE"));
		}
		// and the last has type INVALID_TYPE only
		assertValidation(typeFirst, error("type", "INVALID_TYPE"));
		// and no body contains "Hi" as a value
		for (Response response : notObject) {
			response.then().body(not(containsString("\"Hi\"")));
		}
		typeFirst.then().body(not(containsString("\"Hi\"")));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S5] A NUL character or a lone surrogate in a string value is rejected as payload")
	void aNulCharacterOrALoneSurrogateInAStringIsRejectedWithItsField() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with a NUL in text, a NUL in hint and a lone high surrogate in text
		var nulInText = setup.client().post(setup.items(), raw("test-note", "{\"text\": \"a\\u0000b\"}"));
		var nulInHint = setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"hint\": \"a\\u0000\"}"));
		var surrogate = setup.client().post(setup.items(), raw("test-note", "{\"text\": \"a\\ud800b\"}"));

		// then every response is 400 VALIDATION_FAILED with payload INVALID_FORMAT only
		assertValidation(nulInText, error("payload", "INVALID_FORMAT"));
		assertValidation(nulInHint, error("payload", "INVALID_FORMAT"));
		assertValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value
		nulInText.then().body(not(containsString("a\u0000b"))).body(not(containsString("a\\u0000b")));
		nulInHint.then().body(not(containsString("a\u0000"))).body(not(containsString("a\\u0000")));
		surrogate.then().body(not(containsString("a\ud800b"))).body(not(containsString("a\\ud800b")));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S6] A NUL character in an object key is an unknown property reported as payload")
	void aNulCharacterInAnObjectKeyIsAnUnknownPropertyReportedAsPayload() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type test-note with the payload {"text": "ok", "ke\u0000y": 1}
		var response = setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"ke\\u0000y\": 1}"));

		// then the response is 400 VALIDATION_FAILED with payload UNKNOWN_FIELD only
		assertValidation(response, error("payload", "UNKNOWN_FIELD"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S7] The record's constraints report errors under payload.")
	void theRecordsConstraintsReportErrorsUnderPayload() {
		// given a logged-in teacher A with a part, and the type test-note whose text is required and at most 1000 characters and whose optional hint is at most 200 characters
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with the payloads {}, {"text": null}, {"text": 42}, {"text": ["Привет"]}, a text of 1001 Cyrillic letters and a hint of 201 Cyrillic letters
		Map<String, Object> nullText = new HashMap<>();
		nullText.put("text", null);
		List<Response> responses = List.of(
				setup.client().post(setup.items(), body("test-note", Map.of())),
				setup.client().post(setup.items(), body("test-note", nullText)),
				setup.client().post(setup.items(), body("test-note", Map.of("text", 42))),
				setup.client().post(setup.items(), body("test-note", Map.of("text", List.of("Привет")))),
				setup.client().post(setup.items(), note("Я".repeat(1001))),
				setup.client().post(setup.items(), body("test-note", Map.of("text", "ok", "hint", "Я".repeat(201)))));

		// then the responses are 400 VALIDATION_FAILED with REQUIRED, REQUIRED, INVALID_TYPE, INVALID_TYPE and TOO_LONG for payload.text and TOO_LONG for payload.hint
		assertValidation(responses.get(0), error("payload.text", "REQUIRED"));
		assertValidation(responses.get(1), error("payload.text", "REQUIRED"));
		assertValidation(responses.get(2), error("payload.text", "INVALID_TYPE"));
		assertValidation(responses.get(3), error("payload.text", "INVALID_TYPE"));
		assertValidation(responses.get(4), error("payload.text", "TOO_LONG"));
		assertValidation(responses.get(5), error("payload.hint", "TOO_LONG"));
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
	@DisplayName("[items/R2-S9] The stored payload is the serialised record, without null fields, in its jsonb form")
	void theStoredPayloadIsTheSerialisedRecordWithoutNullFieldsInItsJsonbForm() {
		// given a logged-in teacher A with a part, and the type test-note
		var setup = teacherWithPart();

		// when teacher A adds an item with extra spaces and the key text twice, and an item with the payload {"text": "Hi"}
		String body = "{\"type\": \"test-note\", \"payload\": {\"hint\":  \"Ответ: «мышь»\", \"text\":  \"Привет\", \"text\": \"Мир\"}}";
		var first = setup.client().post(setup.items(), body);
		var second = setup.client().post(setup.items(), note("Hi"));

		// then the first response is 201 with the payload equal to {"text": "Мир", "hint": "Ответ: «мышь»"} and version 1
		first.then().statusCode(201)
				.body("payload", equalTo(Map.of("text", "Мир", "hint", "Ответ: «мышь»")))
				.body("version", equalTo(1));
		// and the stored row has the same payload and the type test-note
		UUID firstId = UUID.fromString(first.path("id"));
		assertEquals("test-note", readItem(firstId).type());
		assertTrue(storedPayloadEquals(firstId, "{\"text\": \"Мир\", \"hint\": \"Ответ: «мышь»\"}"));
		// and the second response and its stored payload are exactly {"text": "Hi"}
		second.then().statusCode(201).body("payload", equalTo(Map.of("text", "Hi")));
		assertTrue(storedPayloadEquals(UUID.fromString(second.path("id")), "{\"text\": \"Hi\"}"));
	}

	@Test
	@DisplayName("[items/R2-S10] An unknown payload property is rejected and stops further checks")
	void anUnknownPayloadPropertyIsRejectedAndStopsFurtherChecks() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type test-note with the payload {"hint": "Ok", "extra": {"deep": [1]}}
		var response = setup.client().post(setup.items(), raw("test-note", "{\"hint\": \"Ok\", \"extra\": {\"deep\": [1]}}"));

		// then the response is 400 VALIDATION_FAILED with payload UNKNOWN_FIELD only
		assertValidation(response, error("payload", "UNKNOWN_FIELD"));
		// and the body contains neither "extra" nor "deep"
		response.then().body(not(containsString("extra"))).body(not(containsString("deep")));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S11] A property of the request body outside the payload is still ignored")
	void aPropertyOfTheRequestBodyOutsideThePayloadIsStillIgnored() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with the body {"type": "test-note", "payload": {"text": "Hi"}, "extra": 1}
		var response = setup.client().post(setup.items(), "{\"type\": \"test-note\", \"payload\": {\"text\": \"Hi\"}, \"extra\": 1}");

		// then the response is 201 and the payload is {"text": "Hi"}
		response.then().statusCode(201).body("payload", equalTo(Map.of("text", "Hi")));
	}

	@Test
	@DisplayName("[items/R2-S12] Decimals keep their precision, out-of-range decimals and a non-integer rating are rejected")
	void decimalsKeepTheirPrecisionOutOfRangeDecimalsAndANonIntegerRatingAreRejected() {
		// given a logged-in teacher A with a part, and the type test-note whose optional rating is an Integer and optional weight a BigDecimal with @Digits(integer = 10, fraction = 20)
		var setup = teacherWithPart();

		// when teacher A adds items of the type test-note with the weight 0.12345678901234567890 and rating 3, then the weights 1e400, 1e200000 and 1e-200000, then the ratings 1.5, 1e400 and "5"
		var precise = setup.client().post(setup.items(),
				raw("test-note", "{\"text\": \"ok\", \"weight\": 0.12345678901234567890, \"rating\": 3}"));
		List<Response> badWeights = List.of(
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"weight\": 1e400}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"weight\": 1e200000}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"weight\": 1e-200000}")));
		List<Response> badRatings = List.of(
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"rating\": 1.5}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"rating\": 1e400}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"rating\": \"5\"}")));

		// then the first response is 201 with weight numerically equal to 0.12345678901234567890 and rating 3
		precise.then().statusCode(201).body("payload.rating", equalTo(3));
		JsonNode returned = EXACT.readTree(precise.asString()).get("payload");
		assertEquals(0, new BigDecimal("0.12345678901234567890").compareTo(returned.get("weight").decimalValue()));
		// and the stored payload has the same value
		String stored = jdbc.sql("select payload->>'weight' from items where part_id = :part")
				.param("part", setup.part())
				.query(String.class)
				.single();
		assertEquals(0, new BigDecimal("0.12345678901234567890").compareTo(new BigDecimal(stored)));
		// and the next three are 400 VALIDATION_FAILED with payload.weight INVALID_FORMAT only
		for (Response response : badWeights) {
			assertValidation(response, error("payload.weight", "INVALID_FORMAT"));
		}
		// and the last three are 400 VALIDATION_FAILED with payload.rating INVALID_TYPE only
		for (Response response : badRatings) {
			assertValidation(response, error("payload.rating", "INVALID_TYPE"));
		}
		// and the part has exactly 1 item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R2-S13] A key is never echoed, even when its value is rejected")
	void aKeyIsNeverEchoedEvenWhenItsValueIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the payloads {"text": "ok", "secretKey": "a\u0000"} and {"text": "ok", "secretKey": "a\ud800"}
		List<Response> responses = List.of(
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"secretKey\": \"a\\u0000\"}")),
				setup.client().post(setup.items(), raw("test-note", "{\"text\": \"ok\", \"secretKey\": \"a\\ud800\"}")));

		// then both responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT only
		for (Response response : responses) {
			assertValidation(response, error("payload", "INVALID_FORMAT"));
			// and no body contains "secretKey"
			response.then().body(not(containsString("secretKey")));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	private boolean storedPayloadEquals(UUID itemId, String expectedJson) {
		return jdbc.sql("select payload = cast(:expected as jsonb) from items where id = :id")
				.param("expected", expectedJson)
				.param("id", itemId)
				.query(Boolean.class)
				.single();
	}
}
