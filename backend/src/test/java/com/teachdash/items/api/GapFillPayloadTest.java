package com.teachdash.items.api;

import static com.teachdash.items.support.GapFillPayloads.EMOJI;
import static com.teachdash.items.support.GapFillPayloads.alternating;
import static com.teachdash.items.support.GapFillPayloads.gap;
import static com.teachdash.items.support.GapFillPayloads.gaps;
import static com.teachdash.items.support.GapFillPayloads.json;
import static com.teachdash.items.support.GapFillPayloads.payload;
import static com.teachdash.items.support.GapFillPayloads.rawSegments;
import static com.teachdash.items.support.GapFillPayloads.text;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-gap-fill R2. */
class GapFillPayloadTest extends ItemApiTest {

	private static final String GAP_GO = "{\"type\": \"gap\", \"answers\": [\"go\"]}";

	private static final String TEXT_A = "{\"type\": \"text\", \"text\": \"A\"}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("gap-fill", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("gap-fill", payloadJson));
	}

	private static String raw(String... segments) {
		return rawSegments(segments);
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
	@DisplayName("[item-gap-fill/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 51 segments, 5 answers of 100 Cyrillic letters, a 500-letter text with a 50-emoji answer, and a 250-emoji text
		var segments51 = payload(alternating(51, "a", 500, true));
		List<String> answers = new ArrayList<>();
		for (String last : List.of("а", "б", "в", "г", "д")) {
			answers.add("Я".repeat(99) + last);
		}
		var fiveAnswers = payload(gap(answers.toArray(String[]::new)));
		var longCyrillic = payload(text("Я".repeat(500)), gap(EMOJI.repeat(50)));
		var longEmojiText = payload(text(EMOJI.repeat(250)), gap("go"));
		var responses = List.of(
				add(setup, segments51),
				add(setup, fiveAnswers),
				add(setup, longCyrillic),
				add(setup, longEmojiText));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), segments51, 1);
		assertCreated(responses.get(1), fiveAnswers, 2);
		assertCreated(responses.get(2), longCyrillic, 3);
		assertCreated(responses.get(3), longEmojiText, 4);
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S2] A text, an answer, an answer list or a segment list over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a 501-letter text, a 251-emoji text, a 101-letter answer, a 51-emoji answer, 6 answers and 52 segments
		var longText = add(setup, payload(text("Я".repeat(501)), gap("go")));
		var longEmojiText = add(setup, payload(text(EMOJI.repeat(251)), gap("go")));
		var longAnswer = add(setup, payload(gap("a".repeat(101))));
		var longEmojiAnswer = add(setup, payload(gap(EMOJI.repeat(51))));
		var sixAnswers = add(setup, payload(gap("a", "b", "c", "d", "e", "f")));
		var segments52 = add(setup, payload(alternating(52, "a", 5, false)));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG errors of the scenario
		expectValidation(longText, error("payload.segments[0].text", "TOO_LONG"));
		expectValidation(longEmojiText, error("payload.segments[0].text", "TOO_LONG"));
		expectValidation(longAnswer, error("payload.segments[0].answers[0]", "TOO_LONG"));
		expectValidation(longEmojiAnswer, error("payload.segments[0].answers[0]", "TOO_LONG"));
		expectValidation(sixAnswers, error("payload.segments[0].answers", "TOO_LONG"));
		expectValidation(segments52, error("payload.segments", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(longText, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmojiText, EMOJI.repeat(5));
		assertNoEcho(longAnswer, "aaaaaaaaaa");
		assertNoEcho(longEmojiAnswer, EMOJI.repeat(5));
		assertNoEcho(sixAnswers);
		assertNoEcho(segments52);
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S3] More than 25 gaps are too many")
	void moreThan25GapsAreTooMany() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with 26 adjacent gaps
		var response = add(setup, payload(gaps(26)));

		// then the response is 400 VALIDATION_FAILED with payload.segments TOO_LONG
		expectValidation(response, error("payload.segments", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S4] A missing or empty text, and a payload without a gap, are required")
	void aMissingOrEmptyTextAndAGaplessPayloadAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text without text, with a null text, with an empty text, with only a text, and with the payloads {"segments": []}, {"segments": null} and {}
		var noText = addRaw(setup, raw(GAP_GO, "{\"type\": \"text\"}"));
		var nullText = addRaw(setup, raw(GAP_GO, "{\"type\": \"text\", \"text\": null}"));
		var emptyText = addRaw(setup, raw(GAP_GO, "{\"type\": \"text\", \"text\": \"\"}"));
		var onlyText = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"No gap here.\"}"));
		var noSegments = addRaw(setup, "{\"segments\": []}");
		var nullSegments = addRaw(setup, "{\"segments\": null}");
		var empty = addRaw(setup, "{}");

		// then the first three responses have payload.segments[1].text REQUIRED
		for (Response response : List.of(noText, nullText, emptyText)) {
			expectValidation(response, error("payload.segments[1].text", "REQUIRED"));
		}
		// and the other four have payload.segments REQUIRED
		for (Response response : List.of(onlyText, noSegments, nullSegments, empty)) {
			expectValidation(response, error("payload.segments", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S5] A gap needs answers, and every answer must be non-blank")
	void aGapNeedsAnswersAndEveryAnswerMustBeNonBlank() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with six bad gaps, each as the second segment after a text segment
		var noAnswers = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\"}"));
		var nullAnswers = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answers\": null}"));
		var emptyAnswers = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answers\": []}"));
		var emptyAnswer = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answers\": [\"go\", \"\"]}"));
		var blankAnswer = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answers\": [\"  \", \"go\"]}"));
		var nullAnswer = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answers\": [\"go\", null]}"));

		// then the first three have payload.segments[1].answers REQUIRED, with no payload.segments error
		for (Response response : List.of(noAnswers, nullAnswers, emptyAnswers)) {
			expectValidation(response, error("payload.segments[1].answers", "REQUIRED"));
		}
		// and the next three have REQUIRED on the answer at index 1, 0 and 1
		expectValidation(emptyAnswer, error("payload.segments[1].answers[1]", "REQUIRED"));
		expectValidation(blankAnswer, error("payload.segments[1].answers[0]", "REQUIRED"));
		expectValidation(nullAnswer, error("payload.segments[1].answers[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S6] A repeated answer in a gap is rejected only on an exact match")
	void aRepeatedAnswerIsRejectedOnlyOnAnExactMatch() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the answers [go, walk, go], [Ёлка, ёлка, елка, Ёлка], and [go, Go, " go", "go "] with [go] in another gap
		var repeated = add(setup, payload(gap("go", "walk", "go")));
		var repeatedCyrillic = add(setup, payload(gap("Ёлка", "ёлка", "елка", "Ёлка")));
		var distinct = payload(gap("go", "Go", " go", "go "), gap("go"));
		var accepted = add(setup, distinct);

		// then the first two are 400 with INVALID_FORMAT on the later answer
		expectValidation(repeated, error("payload.segments[0].answers[2]", "INVALID_FORMAT"));
		expectValidation(repeatedCyrillic, error("payload.segments[0].answers[3]", "INVALID_FORMAT"));
		// and the third is 201 with all answers exactly as sent
		assertCreated(accepted, distinct, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S7] Two adjacent text segments are rejected on the second")
	void twoAdjacentTextSegmentsAreRejectedOnTheSecond() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the segments [text, text, gap] and [gap, text, text, text]
		var firstPair = add(setup, payload(text("A"), text("B"), gap("go")));
		var tripleText = add(setup, payload(gap("go"), text("A"), text("B"), text("C")));

		// then both responses are 400 with INVALID_FORMAT on each text that follows a text
		expectValidation(firstPair, error("payload.segments[1]", "INVALID_FORMAT"));
		expectValidation(
				tripleText,
				error("payload.segments[2]", "INVALID_FORMAT"),
				error("payload.segments[3]", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S8] A field of the other segment type, or an unknown property, is rejected")
	void aFieldOfTheOtherSegmentTypeOrAnUnknownPropertyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a gap with text, a text with answers, a text with empty answers, an unknown segment property, an unknown payload property and a gap with an id
		var gapWithText = addRaw(setup, raw(
				"{\"type\": \"text\", \"text\": \"Hi\"}",
				"{\"type\": \"gap\", \"text\": \"go\", \"answers\": [\"go\"]}"));
		var textWithAnswers = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"answers\": [\"go\"]}", GAP_GO));
		var textWithEmptyAnswers = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"answers\": []}", GAP_GO));
		var unknownInSegment = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"hint\": \"x\"}", GAP_GO));
		var unknownInPayload = addRaw(setup, "{\"segments\": [" + GAP_GO + "], \"title\": \"Intro\"}");
		var gapWithId = addRaw(setup, raw("{\"type\": \"gap\", \"answers\": [\"go\"], \"id\": 1}"));

		// then a gap with text is UNKNOWN_FIELD at payload.segments[1].text
		expectValidation(gapWithText, error("payload.segments[1].text", "UNKNOWN_FIELD"));
		// and a text with answers, also empty, is UNKNOWN_FIELD at payload.segments[0].answers
		expectValidation(textWithAnswers, error("payload.segments[0].answers", "UNKNOWN_FIELD"));
		expectValidation(textWithEmptyAnswers, error("payload.segments[0].answers", "UNKNOWN_FIELD"));
		// and a truly unknown property is UNKNOWN_FIELD at payload, with no name
		for (Response response : List.of(unknownInSegment, unknownInPayload, gapWithId)) {
			expectValidation(response, error("payload", "UNKNOWN_FIELD"));
		}
		assertNoEcho(unknownInSegment, "hint");
		assertNoEcho(unknownInPayload, "title");
		assertNoEcho(gapWithId, "\"id\"");
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S9] A missing, blank or unknown segment type and a null segment are rejected")
	void aBadSegmentTypeAndANullSegmentAreRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a bad segment after a valid gap: no type, a null type, a blank type, "image", "Gap" and null
		var noType = addRaw(setup, raw(GAP_GO, "{\"text\": \"Hi\"}"));
		var nullType = addRaw(setup, raw(GAP_GO, "{\"type\": null, \"text\": \"Hi\"}"));
		var blankType = addRaw(setup, raw(GAP_GO, "{\"type\": \" \", \"text\": \"Hi\"}"));
		var image = addRaw(setup, raw(GAP_GO, "{\"type\": \"image\", \"text\": \"Hi\"}"));
		var capitalised = addRaw(setup, raw(GAP_GO, "{\"type\": \"Gap\", \"answers\": [\"go\"]}"));
		var nullSegment = addRaw(setup, raw(GAP_GO, "null"));

		// then the first three have payload.segments[1].type REQUIRED
		for (Response response : List.of(noType, nullType, blankType)) {
			expectValidation(response, error("payload.segments[1].type", "REQUIRED"));
		}
		// and the next two have payload.segments[1].type INVALID_FORMAT
		for (Response response : List.of(image, capitalised)) {
			expectValidation(response, error("payload.segments[1].type", "INVALID_FORMAT"));
		}
		// and the last has payload.segments[1] REQUIRED
		expectValidation(nullSegment, error("payload.segments[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S10] A value of the wrong JSON type is rejected with its path")
	void aValueOfTheWrongJsonTypeIsRejectedWithItsPath() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the seven wrongly typed payloads of the scenario
		var segmentsString = addRaw(setup, "{\"segments\": \"text\"}");
		var segmentsObject = addRaw(setup, "{\"segments\": {}}");
		var segmentString = addRaw(setup, "{\"segments\": [\"text\"]}");
		var typeNumber = addRaw(setup, "{\"segments\": [{\"type\": 5, \"text\": \"Hi\"}]}");
		var textArray = addRaw(setup, "{\"segments\": [{\"type\": \"text\", \"text\": [\"Привет\"]}]}");
		var answersString = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"answers\": \"go\"}]}");
		var answerNumber = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"answers\": [\"go\", 5]}]}");

		// then every response is 400 VALIDATION_FAILED with INVALID_TYPE at its own path
		expectValidation(segmentsString, error("payload.segments", "INVALID_TYPE"));
		expectValidation(segmentsObject, error("payload.segments", "INVALID_TYPE"));
		expectValidation(segmentString, error("payload.segments[0]", "INVALID_TYPE"));
		expectValidation(typeNumber, error("payload.segments[0].type", "INVALID_TYPE"));
		expectValidation(textArray, error("payload.segments[0].text", "INVALID_TYPE"));
		expectValidation(answersString, error("payload.segments[0].answers", "INVALID_TYPE"));
		expectValidation(answerNumber, error("payload.segments[0].answers[1]", "INVALID_TYPE"));
		// and no body contains "Привет", and the part has no item
		assertNoEcho(textArray, "Привет");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S11] Limits are checked first, the structure rules after, and all structure errors come together")
	void limitsAreCheckedFirstAndStructureErrorsComeTogether() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds a 501-letter text with a repeated answer, two adjacent texts with a repeat and an empty answer, a repeated answer, and a gap with a 501-letter text
		var longTextAndRepeat = add(setup, payload(text("a".repeat(501)), gap("go", "go")));
		var manyStructureErrors = add(setup, payload(text("A"), text("B"), gap("go", "go"), gap("", "x")));
		var onlyRepeat = add(setup, payload(text("A"), gap("go", "go")));
		var gapWithLongText = addRaw(setup, raw(
				TEXT_A,
				"{\"type\": \"gap\", \"answers\": [\"go\"], \"text\": \"" + "a".repeat(501) + "\"}"));

		// then the first has only TOO_LONG on the text, the duplicate not yet reported
		expectValidation(longTextAndRepeat, error("payload.segments[0].text", "TOO_LONG"));
		// and the second has the three structure errors sorted by field
		expectValidation(
				manyStructureErrors,
				error("payload.segments[1]", "INVALID_FORMAT"),
				error("payload.segments[2].answers[1]", "INVALID_FORMAT"),
				error("payload.segments[3].answers[0]", "REQUIRED"));
		// and the third has the repeat only
		expectValidation(onlyRepeat, error("payload.segments[1].answers[1]", "INVALID_FORMAT"));
		// and the fourth has TOO_LONG, not UNKNOWN_FIELD, because Bean Validation runs first
		expectValidation(gapWithLongText, error("payload.segments[1].text", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S12] Whitespace-only text between gaps is accepted")
	void whitespaceOnlyTextBetweenGapsIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text " " between two gaps, and with a line break between gaps and a whitespace-only text last
		var space = payload(text("I "), gap("go"), text(" "), gap("walk"), text(" to school."));
		var lineBreak = payload(gap("go"), text("\n"), gap("walk"), text("  \n\t"));
		var first = add(setup, space);
		var second = add(setup, lineBreak);

		// then both responses are 201 with the segments exactly as sent, in the positions 1 and 2
		assertCreated(first, space, 1);
		assertCreated(second, lineBreak, 2);
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S13] A NUL in an answer and a lone surrogate in a text are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejectedAsPayload() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in an answer and a lone high surrogate in a text, written as JSON escapes
		var nul = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answers\": [\"a\\u0000b\"]}"));
		var surrogate = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"a\\ud800b\"}", GAP_GO));

		// then both responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT
		expectValidation(nul, error("payload", "INVALID_FORMAT"));
		expectValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value, and the part has no item
		assertNoEcho(nul, "a\u0000b", "a\\u0000b");
		assertNoEcho(surrogate, "a\ud800b", "a\\ud800b");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S14] A payload with exactly 25 adjacent gaps is accepted")
	void aPayloadWith25AdjacentGapsIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with 25 adjacent gaps
		var sent = payload(gaps(25));
		var response = add(setup, sent);

		// then the response is 201 with the segments exactly as sent
		assertCreated(response, sent, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill/R2-S15] A null, typeless or unknown-type segment does not count as a gap")
	void aNullTypelessOrUnknownTypeSegmentDoesNotCountAsAGap() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text and an unknown-type segment, a text and null, a text and a typeless segment, and 25 gaps followed by an unknown-type segment
		var unknownType = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\"}", "{\"type\": \"image\"}"));
		var nullSegment = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\"}", "null"));
		var typeless = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\"}", "{\"answers\": [\"go\"]}"));
		List<Object> manyGaps = new ArrayList<>(gaps(25));
		manyGaps.add(Map.of("type", "image"));
		var gapsThenImage = add(setup, payload(manyGaps));

		// then the first three have payload.segments REQUIRED and the error of their segment
		expectValidation(
				unknownType,
				error("payload.segments", "REQUIRED"),
				error("payload.segments[1].type", "INVALID_FORMAT"));
		expectValidation(
				nullSegment,
				error("payload.segments", "REQUIRED"),
				error("payload.segments[1]", "REQUIRED"));
		expectValidation(
				typeless,
				error("payload.segments", "REQUIRED"),
				error("payload.segments[1].type", "REQUIRED"));
		// and the last has only the error of segment 25, with no TOO_LONG
		expectValidation(gapsThenImage, error("payload.segments[25].type", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
