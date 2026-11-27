package com.teachdash.items.api;

import static com.teachdash.items.support.FreeTextPayloads.EMOJI;
import static com.teachdash.items.support.FreeTextPayloads.payload;
import static com.teachdash.items.support.FreeTextPayloads.raw;
import static com.teachdash.items.support.GapFillPayloads.json;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-free-text R2. */
class FreeTextPayloadTest extends ItemApiTest {

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("free-text", payloadJson));
	}

	private Response addWith(Setup setup, String... members) {
		return addRaw(setup, raw("Write.", members));
	}

	private void assertCreated(Response response, Object sent) {
		response.then().statusCode(201).body("payload", equalTo(sent));
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-free-text/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 20000 English letters and both limits, 20000 Cyrillic letters, 10000 emoji with limits 1 and 1, and a no-break space and "x" with minLength 10000
		var english = payload("a".repeat(20000), 1, 10000);
		var cyrillic = payload("Я".repeat(20000));
		var emoji = payload(EMOJI.repeat(10000), 1, 1);
		var noBreak = payload(" x", 10000, null);
		var responses = List.of(
				setup.client().post(setup.items(), body("free-text", english)),
				setup.client().post(setup.items(), body("free-text", cyrillic)),
				setup.client().post(setup.items(), body("free-text", emoji)),
				setup.client().post(setup.items(), body("free-text", noBreak)));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), english);
		assertCreated(responses.get(1), cyrillic);
		assertCreated(responses.get(2), emoji);
		assertCreated(responses.get(3), noBreak);
	}

	@Test
	@DisplayName("[item-free-text/R2-S2] A prompt over 20000 characters is too long")
	void aPromptOver20000CharactersIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a prompt of 20001 Cyrillic letters and of 10001 emoji
		var cyrillic = setup.client().post(setup.items(), body("free-text", payload("Я".repeat(20001))));
		var emoji = setup.client().post(setup.items(), body("free-text", payload(EMOJI.repeat(10001))));

		// then both responses are 400 VALIDATION_FAILED with payload.prompt TOO_LONG
		expectValidation(cyrillic, error("payload.prompt", "TOO_LONG"));
		expectValidation(emoji, error("payload.prompt", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		cyrillic.then().body(not(containsString("rejectedValue"))).body(not(containsString("ЯЯЯЯЯЯЯЯЯЯ")));
		emoji.then().body(not(containsString("rejectedValue"))).body(not(containsString(EMOJI.repeat(5))));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-free-text/R2-S3] A missing or blank prompt is required, and a wrong type or an unknown property is rejected")
	void aMissingOrBlankPromptIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the eight payloads of the scenario
		List<Response> blank = List.of(
						"{}",
						"{\"prompt\": null}",
						"{\"prompt\": \"\"}",
						"{\"prompt\": \"   \"}",
						"{\"prompt\": \"\\n\\t  \"}").stream()
				.map(payload -> addRaw(setup, payload))
				.toList();
		List<Response> wrongType = List.of("{\"prompt\": 5}", "{\"prompt\": [\"Привет\"]}").stream()
				.map(payload -> addRaw(setup, payload))
				.toList();
		var unknown = addRaw(setup, "{\"prompt\": \"ok\", \"hint\": \"Привет\"}");

		// then the first five responses are 400 VALIDATION_FAILED with payload.prompt REQUIRED
		for (Response response : blank) {
			expectValidation(response, error("payload.prompt", "REQUIRED"));
		}
		// and the next two have payload.prompt INVALID_TYPE
		for (Response response : wrongType) {
			expectValidation(response, error("payload.prompt", "INVALID_TYPE"));
		}
		// and the last has payload UNKNOWN_FIELD
		expectValidation(unknown, error("payload", "UNKNOWN_FIELD"));
		// and no body contains "Привет" or "hint"
		for (Response response : List.of(blank.get(0), wrongType.get(1), unknown)) {
			response.then().body(not(containsString("Привет"))).body(not(containsString("hint")));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-free-text/R2-S4] A limit outside 1 to 10000 is an invalid format")
	void aLimitOutsideTheRangeIsAnInvalidFormat() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the prompt "Write." and the seven limit combinations of the scenario
		var minZero = addWith(setup, "\"minLength\": 0");
		var minNegative = addWith(setup, "\"minLength\": -1");
		var minOver = addWith(setup, "\"minLength\": 10001");
		var maxZero = addWith(setup, "\"maxLength\": 0");
		var maxNegative = addWith(setup, "\"maxLength\": -5");
		var maxOver = addWith(setup, "\"maxLength\": 10001");
		var both = addWith(setup, "\"minLength\": 0", "\"maxLength\": 10001");

		// then (a), (b) and (c) are 400 VALIDATION_FAILED with payload.minLength INVALID_FORMAT
		for (Response response : List.of(minZero, minNegative, minOver)) {
			expectValidation(response, error("payload.minLength", "INVALID_FORMAT"));
		}
		// and (d), (e) and (f) with payload.maxLength INVALID_FORMAT
		for (Response response : List.of(maxZero, maxNegative, maxOver)) {
			expectValidation(response, error("payload.maxLength", "INVALID_FORMAT"));
		}
		// and (g) with both errors, payload.maxLength before payload.minLength
		expectValidation(both, error("payload.maxLength", "INVALID_FORMAT"), error("payload.minLength", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-free-text/R2-S5] A limit that is not an integer is an invalid type")
	void aLimitThatIsNotAnIntegerIsAnInvalidType() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the prompt "Write." and the six non-integer limits of the scenario
		var minString = addWith(setup, "\"minLength\": \"5\"");
		var minFloat = addWith(setup, "\"minLength\": 5.5");
		var minBoolean = addWith(setup, "\"minLength\": true");
		var maxEmpty = addWith(setup, "\"maxLength\": \"\"");
		var maxString = addWith(setup, "\"maxLength\": \"10\"");
		var maxList = addWith(setup, "\"maxLength\": [10]");

		// then (a), (b) and (c) are 400 VALIDATION_FAILED with payload.minLength INVALID_TYPE
		for (Response response : List.of(minString, minFloat, minBoolean)) {
			expectValidation(response, error("payload.minLength", "INVALID_TYPE"));
		}
		// and (d), (e) and (f) with payload.maxLength INVALID_TYPE
		for (Response response : List.of(maxEmpty, maxString, maxList)) {
			expectValidation(response, error("payload.maxLength", "INVALID_TYPE"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-free-text/R2-S6] A minimum above the maximum is rejected at the maximum, an equal or single limit is accepted")
	void aMinimumAboveTheMaximumIsRejectedAtTheMaximum() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the prompt "Write." and the five limit combinations of the scenario
		var above = addWith(setup, "\"minLength\": 10", "\"maxLength\": 9");
		var extreme = addWith(setup, "\"minLength\": 10000", "\"maxLength\": 1");
		var equal = addWith(setup, "\"minLength\": 7", "\"maxLength\": 7");
		var minOnly = addWith(setup, "\"minLength\": 9999");
		var maxOnly = addWith(setup, "\"maxLength\": 1");

		// then (a) and (b) are 400 VALIDATION_FAILED with payload.maxLength INVALID_FORMAT and no value echoed
		for (Response response : List.of(above, extreme)) {
			expectValidation(response, error("payload.maxLength", "INVALID_FORMAT"));
			response.then().body(not(containsString("rejectedValue")));
		}
		// and (c), (d) and (e) are 201 with the payload exactly as sent
		assertCreated(equal, payload("Write.", 7, 7));
		assertCreated(minOnly, payload("Write.", 9999, null));
		assertCreated(maxOnly, payload("Write.", null, 1));
		// and the part has exactly three items
		assertEquals(3, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-free-text/R2-S7] The order of the limits is checked only after the other errors are fixed")
	void theOrderIsCheckedOnlyAfterTheOtherErrors() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a blank prompt and minLength above maxLength, and with minLength 20 and maxLength 0
		var blank = addRaw(setup, "{\"prompt\": \"  \", \"minLength\": 10, \"maxLength\": 5}");
		var outOfRange = addWith(setup, "\"minLength\": 20", "\"maxLength\": 0");

		// then (a) is 400 with only payload.prompt REQUIRED
		expectValidation(blank, error("payload.prompt", "REQUIRED"));
		// and (b) is 400 with only payload.maxLength INVALID_FORMAT
		expectValidation(outOfRange, error("payload.maxLength", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
