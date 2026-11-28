package com.teachdash.items.api;

import static com.teachdash.items.support.MatchingPayloads.EMOJI;
import static com.teachdash.items.support.MatchingPayloads.json;
import static com.teachdash.items.support.MatchingPayloads.numberedPairs;
import static com.teachdash.items.support.MatchingPayloads.pair;
import static com.teachdash.items.support.MatchingPayloads.pairs;
import static com.teachdash.items.support.MatchingPayloads.payload;
import static com.teachdash.items.support.MatchingPayloads.rawPairs;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-matching R2. */
class MatchingPayloadTest extends ItemApiTest {

	private static final String VALID_PAIR = "{\"left\": \"a\", \"right\": \"b\"}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("matching", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("matching", payloadJson));
	}

	private void assertCreated(Response response, Object sent, int position) {
		response.then().statusCode(201)
				.body("payload", equalTo(sent))
				.body("position", equalTo(position));
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	private static void assertNoEcho(Response response, String... fragments) {
		response.then().body(not(containsString("rejectedValue")));
		for (String fragment : fragments) {
			response.then().body(not(containsString(fragment)));
		}
	}

	@Test
	@DisplayName("[item-matching/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items: 2 pairs, 25 pairs, 100-letter Cyrillic values and 50-emoji values
		var twoPairs = payload(pair("a", "b"), pair("c", "d"));
		var twentyFive = payload(numberedPairs(25));
		var cyrillic = payload(pair("Я".repeat(100), "Я".repeat(100)), pair("x", "y"));
		var emoji = payload(pair(EMOJI.repeat(50), EMOJI.repeat(50)), pair("x", "y"));
		var responses = List.of(
				add(setup, twoPairs),
				add(setup, twentyFive),
				add(setup, cyrillic),
				add(setup, emoji));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), twoPairs, 1);
		assertCreated(responses.get(1), twentyFive, 2);
		assertCreated(responses.get(2), cyrillic, 3);
		assertCreated(responses.get(3), emoji, 4);
	}

	@Test
	@DisplayName("[item-matching/R2-S2] A left, a right or a list of pairs over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds five items, each over one limit
		var pairs26 = add(setup, payload(numberedPairs(26)));
		var longLeft = add(setup, payload(pair("a".repeat(101), "t"), pair("w2", "t2")));
		var longCyrillicRight = add(setup, payload(pair("w", "Я".repeat(101)), pair("w2", "t2")));
		var longEmojiLeft = add(setup, payload(pair(EMOJI.repeat(51), "t"), pair("w2", "t2")));
		var longEmojiRight = add(setup, payload(pair("w", EMOJI.repeat(51)), pair("w2", "t2")));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG error of the scenario
		expectValidation(pairs26, error("payload.pairs", "TOO_LONG"));
		expectValidation(longLeft, error("payload.pairs[0].left", "TOO_LONG"));
		expectValidation(longCyrillicRight, error("payload.pairs[0].right", "TOO_LONG"));
		expectValidation(longEmojiLeft, error("payload.pairs[0].left", "TOO_LONG"));
		expectValidation(longEmojiRight, error("payload.pairs[0].right", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(pairs26);
		assertNoEcho(longLeft, "aaaaaaaaaa");
		assertNoEcho(longCyrillicRight, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmojiLeft, EMOJI.repeat(5));
		assertNoEcho(longEmojiRight, EMOJI.repeat(5));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S3] Missing, null or empty pairs are required, and one pair is too short")
	void missingNullOrEmptyPairsAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the payloads {}, {"pairs": null}, {"pairs": []} and one pair
		var missing = addRaw(setup, "{}");
		var nullPairs = addRaw(setup, "{\"pairs\": null}");
		var empty = addRaw(setup, "{\"pairs\": []}");
		var onePair = addRaw(setup, rawPairs(VALID_PAIR));

		// then the first three are 400 with payload.pairs REQUIRED and the last with payload.pairs TOO_SHORT
		expectValidation(missing, error("payload.pairs", "REQUIRED"));
		expectValidation(nullPairs, error("payload.pairs", "REQUIRED"));
		expectValidation(empty, error("payload.pairs", "REQUIRED"));
		expectValidation(onePair, error("payload.pairs", "TOO_SHORT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S4] A null pair is required at its index")
	void aNullPairIsRequiredAtItsIndex() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a null second pair, and with two null first pairs
		var nullSecond = add(setup, payload(pair("a", "b"), null));
		var nullFirstTwo = add(setup, payload(null, null, pair("a", "b")));

		// then both responses are 400 with the REQUIRED errors at the null indexes
		expectValidation(nullSecond, error("payload.pairs[1]", "REQUIRED"));
		expectValidation(
				nullFirstTwo,
				error("payload.pairs[0]", "REQUIRED"),
				error("payload.pairs[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S5] A missing, null, empty or blank left or right is required")
	void aMissingOrBlankLeftOrRightIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds seven items, each with a bad second pair after the valid first pair
		var noLeft = addRaw(setup, rawPairs(VALID_PAIR, "{\"right\": \"d\"}"));
		var nullLeft = addRaw(setup, rawPairs(VALID_PAIR, "{\"left\": null, \"right\": \"d\"}"));
		var emptyLeft = addRaw(setup, rawPairs(VALID_PAIR, "{\"left\": \"\", \"right\": \"d\"}"));
		var blankRight = addRaw(setup, rawPairs(VALID_PAIR, "{\"left\": \"c\", \"right\": \"  \"}"));
		var whitespaceRight = addRaw(setup, rawPairs(VALID_PAIR, "{\"left\": \"c\", \"right\": \"\\n\\t\"}"));
		var noRight = addRaw(setup, rawPairs(VALID_PAIR, "{\"left\": \"c\"}"));
		var bothBlank = addRaw(setup, rawPairs(VALID_PAIR, "{\"left\": \" \", \"right\": \"\"}"));

		// then the first three have payload.pairs[1].left REQUIRED
		for (Response response : List.of(noLeft, nullLeft, emptyLeft)) {
			expectValidation(response, error("payload.pairs[1].left", "REQUIRED"));
		}
		// and the next three have payload.pairs[1].right REQUIRED
		for (Response response : List.of(blankRight, whitespaceRight, noRight)) {
			expectValidation(response, error("payload.pairs[1].right", "REQUIRED"));
		}
		// and the last has both
		expectValidation(
				bothBlank,
				error("payload.pairs[1].left", "REQUIRED"),
				error("payload.pairs[1].right", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S6] A repeated left or right is rejected on the later one")
	void aRepeatedLeftOrRightIsRejectedOnTheLaterOne() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items with repeats
		var repeatedLeft = add(setup, payload(pair("go", "идти"), pair("walk", "ходить"), pair("go", "ехать")));
		var repeatedRight = add(setup, payload(pair("go", "идти"), pair("walk", "идти")));
		var repeatedBoth = add(setup, payload(pair("go", "идти"), pair("go", "идти")));
		var repeatedThrice = add(setup, payload(pair("a", "x"), pair("a", "y"), pair("a", "z")));

		// then each is 400 with INVALID_FORMAT on the later value only
		expectValidation(repeatedLeft, error("payload.pairs[2].left", "INVALID_FORMAT"));
		expectValidation(repeatedRight, error("payload.pairs[1].right", "INVALID_FORMAT"));
		expectValidation(
				repeatedBoth,
				error("payload.pairs[1].left", "INVALID_FORMAT"),
				error("payload.pairs[1].right", "INVALID_FORMAT"));
		expectValidation(
				repeatedThrice,
				error("payload.pairs[1].left", "INVALID_FORMAT"),
				error("payload.pairs[2].left", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S7] Only an exact repeat is a duplicate, and a left may equal a right")
	void onlyAnExactRepeatIsADuplicate() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item whose lefts differ by case and spaces and whose rights differ by "ё" and "е" and spaces
		var sent = payload(
				pair("Go", "ёлка"),
				pair("go", "елка"),
				pair(" go", "Ёлка"),
				pair("go ", "Ёлка "));
		var response = add(setup, sent);

		// then the response is 201 with all four pairs exactly as sent
		assertCreated(response, sent, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S8] A value with its own error is not compared, the other side of its pair still is")
	void aValueWithItsOwnErrorIsNotCompared() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds three items
		var blankLefts = add(setup, payload(pair("a", "x"), pair("", "y"), pair("", "z")));
		var longLefts = add(setup, payload(pair("a".repeat(101), "x"), pair("a".repeat(101), "y")));
		var repeatedLeftAndBlankRight = add(setup, payload(pair("go", "a"), pair("go", "")));

		// then the blank lefts are REQUIRED and not compared
		expectValidation(
				blankLefts,
				error("payload.pairs[1].left", "REQUIRED"),
				error("payload.pairs[2].left", "REQUIRED"));
		// and the long lefts are TOO_LONG, with no INVALID_FORMAT
		expectValidation(
				longLefts,
				error("payload.pairs[0].left", "TOO_LONG"),
				error("payload.pairs[1].left", "TOO_LONG"));
		// and the repeated left is still compared next to a blank right
		expectValidation(
				repeatedLeftAndBlankRight,
				error("payload.pairs[1].left", "INVALID_FORMAT"),
				error("payload.pairs[1].right", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S9] A value of the wrong JSON type or an unknown property is rejected")
	void aWrongJsonTypeOrUnknownPropertyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with wrongly typed payloads and with unknown properties
		var pairsString = addRaw(setup, "{\"pairs\": \"text\"}");
		var pairsObject = addRaw(setup, "{\"pairs\": {}}");
		var pairString = addRaw(setup, "{\"pairs\": [\"text\"]}");
		var leftNumber = addRaw(setup, rawPairs("{\"left\": 5, \"right\": \"b\"}"));
		var rightArray = addRaw(setup, rawPairs("{\"left\": \"a\", \"right\": [\"Привет\"]}"));
		Map<String, Object> withCaption = new LinkedHashMap<>(payload(pair("a", "b"), pair("c", "d")));
		withCaption.put("caption", "Intro");
		var unknownInPayload = add(setup, withCaption);
		var unknownInPair = addRaw(setup, rawPairs(
				"{\"left\": \"a\", \"right\": \"b\", \"id\": \"p-7731\"}",
				"{\"left\": \"c\", \"right\": \"d\"}"));

		// then the errors are the INVALID_TYPE and UNKNOWN_FIELD errors of the scenario
		expectValidation(pairsString, error("payload.pairs", "INVALID_TYPE"));
		expectValidation(pairsObject, error("payload.pairs", "INVALID_TYPE"));
		expectValidation(pairString, error("payload.pairs[0]", "INVALID_TYPE"));
		expectValidation(leftNumber, error("payload.pairs[0].left", "INVALID_TYPE"));
		expectValidation(rightArray, error("payload.pairs[0].right", "INVALID_TYPE"));
		expectValidation(unknownInPayload, error("payload", "UNKNOWN_FIELD"));
		expectValidation(unknownInPair, error("payload", "UNKNOWN_FIELD"));
		// and the unknown names and the sent text are not echoed
		assertNoEcho(rightArray, "Привет");
		assertNoEcho(unknownInPayload, "caption", "Intro");
		assertNoEcho(unknownInPair, "p-7731");
		unknownInPair.then()
				.body(not(containsString("Unrecognized")))
				.body(not(containsString("\"id\"")));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S10] The pair count is checked first, the pair rules after, and all pair errors come together")
	void thePairCountIsCheckedFirst() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds 26 pairs with a blank first left, one blank pair, and pairs with a repeat, a blank right and a null pair
		var manyWithBlank = numberedPairs(26);
		manyWithBlank.set(0, pair("", "t1"));
		var tooLong = add(setup, payload(manyWithBlank));
		var singleBlank = add(setup, payload(pair("", "b")));
		var together = add(setup, payload(pairs(pair("go", "a"), pair("go", " "), null)));

		// then the first is TOO_LONG only and the second TOO_SHORT only
		expectValidation(tooLong, error("payload.pairs", "TOO_LONG"));
		expectValidation(singleBlank, error("payload.pairs", "TOO_SHORT"));
		// and the third has all three pair errors, sorted by field
		expectValidation(
				together,
				error("payload.pairs[1].left", "INVALID_FORMAT"),
				error("payload.pairs[1].right", "REQUIRED"),
				error("payload.pairs[2]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-matching/R2-S11] A NUL and a lone surrogate in a value are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in a left and a lone high surrogate in a right (JSON escapes)
		var nul = addRaw(setup, rawPairs(
				"{\"left\": \"a\\u0000b\", \"right\": \"x\"}",
				"{\"left\": \"c\", \"right\": \"y\"}"));
		var surrogate = addRaw(setup, rawPairs(
				"{\"left\": \"a\", \"right\": \"x\"}",
				"{\"left\": \"b\", \"right\": \"y\\ud800z\"}"));

		// then both responses are 400 with payload INVALID_FORMAT
		expectValidation(nul, error("payload", "INVALID_FORMAT"));
		expectValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value
		assertNoEcho(nul, "a\\u0000b", "a\u0000b");
		assertNoEcho(surrogate, "y\\ud800z", "\\uD800", "\\ud800", "\\UD800", "\uFFFD");
		assertFalse(surrogate.asString().toLowerCase().contains("ud800"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
