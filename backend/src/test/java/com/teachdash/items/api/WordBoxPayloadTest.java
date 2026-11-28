package com.teachdash.items.api;

import static com.teachdash.items.support.WordBoxPayloads.EMOJI;
import static com.teachdash.items.support.WordBoxPayloads.alternating;
import static com.teachdash.items.support.WordBoxPayloads.extras;
import static com.teachdash.items.support.WordBoxPayloads.gap;
import static com.teachdash.items.support.WordBoxPayloads.gaps;
import static com.teachdash.items.support.WordBoxPayloads.json;
import static com.teachdash.items.support.WordBoxPayloads.payload;
import static com.teachdash.items.support.WordBoxPayloads.payloadWith;
import static com.teachdash.items.support.WordBoxPayloads.rawSegments;
import static com.teachdash.items.support.WordBoxPayloads.rawSegmentsWith;
import static com.teachdash.items.support.WordBoxPayloads.segments;
import static com.teachdash.items.support.WordBoxPayloads.text;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-gap-fill-word-box R2. */
class WordBoxPayloadTest extends ItemApiTest {

	private static final String GAP_GO = "{\"type\": \"gap\", \"answer\": \"go\"}";

	private static final String TEXT_A = "{\"type\": \"text\", \"text\": \"A\"}";

	private static final String TEXT_HI = "{\"type\": \"text\", \"text\": \"Hi\"}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("gap-fill-word-box", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("gap-fill-word-box", payloadJson));
	}

	private static String raw(String... segments) {
		return rawSegments(segments);
	}

	private static String rawWith(String extraWordsJson, String... segments) {
		return rawSegmentsWith(extraWordsJson, segments);
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
	@DisplayName("[item-gap-fill-word-box/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items at the limits: 51 segments with 10 extra words, 100-letter Cyrillic words, 50-emoji words, a 250-emoji text with ten 100-letter extra words
		var segments51 = payloadWith(alternating(51, "a", 500, true), extras(10));
		var cyrillicWords = payloadWith(segments(gap("Я".repeat(100))), "Ж".repeat(100));
		var emojiWords = payloadWith(
				segments(text("Я".repeat(500)), gap(EMOJI.repeat(50))),
				"😁".repeat(50));
		String[] tenWords = new String[10];
		for (int i = 0; i < 10; i++) {
			tenWords[i] = "b".repeat(99) + i;
		}
		var emojiTextAndTenWords = payloadWith(segments(text(EMOJI.repeat(250)), gap("go")), tenWords);
		var responses = List.of(
				add(setup, segments51),
				add(setup, cyrillicWords),
				add(setup, emojiWords),
				add(setup, emojiTextAndTenWords));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), segments51, 1);
		assertCreated(responses.get(1), cyrillicWords, 2);
		assertCreated(responses.get(2), emojiWords, 3);
		assertCreated(responses.get(3), emojiTextAndTenWords, 4);
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S2] A text, an answer, an extra word, an extra-word list, a segment list or a gap count over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds nine items, each over one limit
		var longText = add(setup, payload(text("Я".repeat(501)), gap("go")));
		var longEmojiText = add(setup, payload(text(EMOJI.repeat(251)), gap("go")));
		var longAnswer = add(setup, payload(gap("a".repeat(101))));
		var longEmojiAnswer = add(setup, payload(gap(EMOJI.repeat(51))));
		var elevenExtras = add(setup, payloadWith(segments(gap("go")), extras(11)));
		var longExtra = add(setup, payloadWith(segments(gap("go")), "a".repeat(101)));
		var longEmojiExtra = add(setup, payloadWith(segments(gap("go")), EMOJI.repeat(51)));
		var segments52 = add(setup, payload(alternating(52, "a", 5, false)));
		var gaps26 = add(setup, payload(gaps(26)));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG error of the scenario
		expectValidation(longText, error("payload.segments[0].text", "TOO_LONG"));
		expectValidation(longEmojiText, error("payload.segments[0].text", "TOO_LONG"));
		expectValidation(longAnswer, error("payload.segments[0].answer", "TOO_LONG"));
		expectValidation(longEmojiAnswer, error("payload.segments[0].answer", "TOO_LONG"));
		expectValidation(elevenExtras, error("payload.extraWords", "TOO_LONG"));
		expectValidation(longExtra, error("payload.extraWords[0]", "TOO_LONG"));
		expectValidation(longEmojiExtra, error("payload.extraWords[0]", "TOO_LONG"));
		expectValidation(segments52, error("payload.segments", "TOO_LONG"));
		expectValidation(gaps26, error("payload.segments", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(longText, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmojiText, EMOJI.repeat(5));
		assertNoEcho(longAnswer, "aaaaaaaaaa");
		assertNoEcho(longEmojiAnswer, EMOJI.repeat(5));
		assertNoEcho(elevenExtras);
		assertNoEcho(longExtra, "aaaaaaaaaa");
		assertNoEcho(longEmojiExtra, EMOJI.repeat(5));
		assertNoEcho(segments52);
		assertNoEcho(gaps26);
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S3] A missing or empty text, and a payload without a gap, are required")
	void aMissingOrEmptyTextAndAGaplessPayloadAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text without text, a null text, an empty text, only a text, and the payloads {"segments": []}, {"segments": null} and {}
		var noText = addRaw(setup, raw(GAP_GO, "{\"type\": \"text\"}"));
		var nullText = addRaw(setup, raw(GAP_GO, "{\"type\": \"text\", \"text\": null}"));
		var emptyText = addRaw(setup, raw(GAP_GO, "{\"type\": \"text\", \"text\": \"\"}"));
		var onlyText = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"No gap here.\"}"));
		var noSegments = addRaw(setup, "{\"segments\": []}");
		var nullSegments = addRaw(setup, "{\"segments\": null}");
		var empty = addRaw(setup, "{}");

		// then the first three have payload.segments[1].text REQUIRED
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
	@DisplayName("[item-gap-fill-word-box/R2-S4] A gap needs an answer that is not blank")
	void aGapNeedsAnAnswerThatIsNotBlank() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items with a gap without answer, with a null answer, an empty answer and a blank answer, each after a text "Hi"
		var noAnswer = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\"}"));
		var nullAnswer = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\", \"answer\": null}"));
		var emptyAnswer = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\", \"answer\": \"\"}"));
		var blankAnswer = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\", \"answer\": \"  \\n\"}"));

		// then all four have payload.segments[1].answer REQUIRED and no payload.segments error
		for (Response response : List.of(noAnswer, nullAnswer, emptyAnswer, blankAnswer)) {
			expectValidation(response, error("payload.segments[1].answer", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S5] Every extra word must be non-blank, and a missing or empty list is accepted")
	void everyExtraWordMustBeNonBlank() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();
		String textHiSpace = "{\"type\": \"text\", \"text\": \"Hi \"}";

		// when teacher A adds items with extraWords ["x", ""], ["  ", "x"], ["x", null], [null], and then [] and null
		var emptyWord = addRaw(setup, rawWith("[\"x\", \"\"]", textHiSpace, GAP_GO));
		var blankWord = addRaw(setup, rawWith("[\"  \", \"x\"]", textHiSpace, GAP_GO));
		var nullSecond = addRaw(setup, rawWith("[\"x\", null]", textHiSpace, GAP_GO));
		var onlyNull = addRaw(setup, rawWith("[null]", textHiSpace, GAP_GO));
		var emptyList = addRaw(setup, rawWith("[]", textHiSpace, GAP_GO));
		var nullList = addRaw(setup, rawWith("null", textHiSpace, GAP_GO));

		// then the first four are 400 with REQUIRED at extraWords[1], [0], [1] and [0]
		expectValidation(emptyWord, error("payload.extraWords[1]", "REQUIRED"));
		expectValidation(blankWord, error("payload.extraWords[0]", "REQUIRED"));
		expectValidation(nullSecond, error("payload.extraWords[1]", "REQUIRED"));
		expectValidation(onlyNull, error("payload.extraWords[0]", "REQUIRED"));
		// and the last two are 201, an empty list is stored as [] and a null is dropped
		emptyList.then().statusCode(201).body("payload.extraWords", equalTo(List.of()));
		nullList.then().statusCode(201).body("payload", not(hasKey("extraWords")));
		// and the part has exactly two items
		assertEquals(2, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S6] A repeated extra word, or an extra word equal to an answer, is rejected only on an exact match")
	void aRepeatedExtraWordOrOneEqualToAnAnswerIsRejectedOnlyOnAnExactMatch() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds five items
		var repeated = add(setup, payloadWith(segments(text("I "), gap("go")), "walk", "run", "walk"));
		var equalToAnswer = add(setup, payloadWith(segments(text("I "), gap("go")), "go"));
		var equalToSecondGap = add(setup, payloadWith(
				segments(text("I "), gap("go"), text(" and "), gap("ran")),
				"x", "ran"));
		var cyrillic = add(setup, payloadWith(segments(text("Мой "), gap("Ёж")), "ёж", "еж", "Ёж"));
		var distinct = payloadWith(
				segments(text("I "), gap("go"), text(" and "), gap("go"), text(" "), gap("Go")),
				"go ", " go", "GO", "Ёж", "ёж", "еж");
		var accepted = add(setup, distinct);

		// then the first four are 400 with INVALID_FORMAT at the later or the equal extra word
		expectValidation(repeated, error("payload.extraWords[2]", "INVALID_FORMAT"));
		expectValidation(equalToAnswer, error("payload.extraWords[0]", "INVALID_FORMAT"));
		expectValidation(equalToSecondGap, error("payload.extraWords[1]", "INVALID_FORMAT"));
		expectValidation(cyrillic, error("payload.extraWords[2]", "INVALID_FORMAT"));
		// and the fifth is 201 with all words exactly as sent
		assertCreated(accepted, distinct, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S7] Two adjacent text segments are rejected on the second")
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
	@DisplayName("[item-gap-fill-word-box/R2-S8] A field of the other segment type, or an unknown property, is rejected")
	void aFieldOfTheOtherSegmentTypeOrAnUnknownPropertyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds six items
		var gapWithText = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\", \"text\": \"go\", \"answer\": \"go\"}"));
		var textWithAnswer = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"answer\": \"go\"}", GAP_GO));
		var gapWithAnswers = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\", \"answers\": [\"go\"]}"));
		var unknownInPayload = addRaw(setup, "{\"segments\": [" + TEXT_HI + ", " + GAP_GO + "], \"options\": [\"a\"]}");
		var gapWithId = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\", \"id\": \"seg-7731\", \"answer\": \"go\"}"));
		var gapWithLongText = addRaw(setup, raw(
				TEXT_HI,
				"{\"type\": \"gap\", \"text\": \"" + "a".repeat(501) + "\", \"answer\": \"go\"}"));

		// then a gap with text is UNKNOWN_FIELD at payload.segments[1].text
		expectValidation(gapWithText, error("payload.segments[1].text", "UNKNOWN_FIELD"));
		// and a text with an answer is UNKNOWN_FIELD at payload.segments[0].answer
		expectValidation(textWithAnswer, error("payload.segments[0].answer", "UNKNOWN_FIELD"));
		// and a truly unknown property is UNKNOWN_FIELD at payload, with no name
		for (Response response : List.of(gapWithAnswers, unknownInPayload, gapWithId)) {
			expectValidation(response, error("payload", "UNKNOWN_FIELD"));
		}
		assertNoEcho(gapWithAnswers, "answers");
		assertNoEcho(unknownInPayload, "options");
		assertNoEcho(gapWithId, "seg-7731");
		// and a gap with an over-long text is TOO_LONG, not UNKNOWN_FIELD, because Bean Validation runs first
		expectValidation(gapWithLongText, error("payload.segments[1].text", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S9] A missing, blank or unknown segment type and a null segment are rejected")
	void aBadSegmentTypeAndANullSegmentAreRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a bad segment after a valid gap: no type, a null type, a blank type, "image", "Gap" and null
		var noType = addRaw(setup, raw(GAP_GO, "{\"text\": \"Hi\"}"));
		var nullType = addRaw(setup, raw(GAP_GO, "{\"type\": null, \"text\": \"Hi\"}"));
		var blankType = addRaw(setup, raw(GAP_GO, "{\"type\": \" \", \"text\": \"Hi\"}"));
		var image = addRaw(setup, raw(GAP_GO, "{\"type\": \"image\", \"text\": \"Hi\"}"));
		var capitalised = addRaw(setup, raw(GAP_GO, "{\"type\": \"Gap\", \"answer\": \"go\"}"));
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
	@DisplayName("[item-gap-fill-word-box/R2-S10] A value of the wrong JSON type is rejected with its path")
	void aValueOfTheWrongJsonTypeIsRejectedWithItsPath() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the ten wrongly typed payloads of the scenario
		var segmentsString = addRaw(setup, "{\"segments\": \"text\"}");
		var segmentsObject = addRaw(setup, "{\"segments\": {}}");
		var segmentString = addRaw(setup, "{\"segments\": [\"text\"]}");
		var typeNumber = addRaw(setup, "{\"segments\": [{\"type\": 5, \"text\": \"Hi\"}]}");
		var textArray = addRaw(setup, "{\"segments\": [{\"type\": \"text\", \"text\": [\"Привет\"]}]}");
		var answerNumber = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"answer\": 5}]}");
		var answerArray = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"answer\": [\"go\"]}]}");
		var extrasString = addRaw(setup, "{\"segments\": [" + GAP_GO + "], \"extraWords\": \"went\"}");
		var extrasObject = addRaw(setup, "{\"segments\": [" + GAP_GO + "], \"extraWords\": {}}");
		var extraNumber = addRaw(setup, "{\"segments\": [" + GAP_GO + "], \"extraWords\": [\"went\", 5]}");

		// then every response is 400 VALIDATION_FAILED with INVALID_TYPE at its own path
		expectValidation(segmentsString, error("payload.segments", "INVALID_TYPE"));
		expectValidation(segmentsObject, error("payload.segments", "INVALID_TYPE"));
		expectValidation(segmentString, error("payload.segments[0]", "INVALID_TYPE"));
		expectValidation(typeNumber, error("payload.segments[0].type", "INVALID_TYPE"));
		expectValidation(textArray, error("payload.segments[0].text", "INVALID_TYPE"));
		expectValidation(answerNumber, error("payload.segments[0].answer", "INVALID_TYPE"));
		expectValidation(answerArray, error("payload.segments[0].answer", "INVALID_TYPE"));
		expectValidation(extrasString, error("payload.extraWords", "INVALID_TYPE"));
		expectValidation(extrasObject, error("payload.extraWords", "INVALID_TYPE"));
		expectValidation(extraNumber, error("payload.extraWords[1]", "INVALID_TYPE"));
		// and no body contains "Привет", and the part has no item
		assertNoEcho(textArray, "Привет");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S11] Limits are checked first, the structure rules after, and all structure errors come together")
	void limitsAreCheckedFirstAndStructureErrorsComeTogether() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds five items
		var longTextAndRepeat = add(setup, payloadWith(segments(text("a".repeat(501)), gap("go")), "x", "x"));
		var manyStructureErrors = addRaw(setup, rawWith(
				"[\"go\", \"w\", \"w\", \"\"]",
				TEXT_A,
				"{\"type\": \"text\", \"text\": \"B\"}",
				GAP_GO,
				"{\"type\": \"gap\", \"answer\": \"\"}",
				GAP_GO));
		var onlyEqualToAnswer = add(setup, payloadWith(segments(text("A"), gap("go")), "go"));
		var gapWithLongText = addRaw(setup, raw(
				TEXT_A,
				"{\"type\": \"gap\", \"text\": \"" + "a".repeat(501) + "\", \"answer\": \"go\"}"));
		List<String> elevenExtras = new ArrayList<>(List.of(extras(10)));
		elevenExtras.add("a".repeat(101));
		var overLimitsAndLongWords = add(setup, payloadWith(
				segments(text("A"), gap("a".repeat(101))),
				elevenExtras.toArray(String[]::new)));

		var adjacentTextsAndLongAnswer = add(setup, payloadWith(segments(text("A"), text("B"), gap("a".repeat(101)))));

		// then the first has only TOO_LONG on the text, the duplicate extra word not yet reported
		expectValidation(longTextAndRepeat, error("payload.segments[0].text", "TOO_LONG"));
		// and the second has the five structure errors sorted by field
		expectValidation(
				manyStructureErrors,
				error("payload.extraWords[0]", "INVALID_FORMAT"),
				error("payload.extraWords[2]", "INVALID_FORMAT"),
				error("payload.extraWords[3]", "REQUIRED"),
				error("payload.segments[1]", "INVALID_FORMAT"),
				error("payload.segments[3].answer", "REQUIRED"));
		// and the third has the equal-to-answer error only
		expectValidation(onlyEqualToAnswer, error("payload.extraWords[0]", "INVALID_FORMAT"));
		// and the fourth has TOO_LONG, not UNKNOWN_FIELD, because Bean Validation runs first
		expectValidation(gapWithLongText, error("payload.segments[1].text", "TOO_LONG"));
		// and the fifth has only TOO_LONG on extraWords, the over-long answer and extra word not yet reported
		expectValidation(overLimitsAndLongWords, error("payload.extraWords", "TOO_LONG"));
		// and the sixth reports the adjacent texts and the long answer together
		expectValidation(
				adjacentTextsAndLongAnswer,
				error("payload.segments[1]", "INVALID_FORMAT"),
				error("payload.segments[2].answer", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S12] A NUL in a word and a lone surrogate in a text are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejectedAsPayload() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in an answer, a NUL in an extra word and a lone high surrogate in a text, written as JSON escapes
		var nulInAnswer = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"answer\": \"a\\u0000b\"}"));
		var nulInExtra = addRaw(setup, rawWith("[\"x\\u0000y\"]", TEXT_A, GAP_GO));
		var surrogate = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"a\\ud800b\"}", GAP_GO));

		// then all three responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT
		expectValidation(nulInAnswer, error("payload", "INVALID_FORMAT"));
		expectValidation(nulInExtra, error("payload", "INVALID_FORMAT"));
		expectValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value, and the part has no item
		assertNoEcho(nulInAnswer, "a\u0000b", "a\\u0000b");
		assertNoEcho(nulInExtra, "x\u0000y", "x\\u0000y");
		assertNoEcho(surrogate, "a\ud800b", "a\\ud800b");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R2-S13] A null, typeless or unknown-type segment does not count as a gap, a gap with a missing answer does")
	void aNullTypelessOrUnknownTypeSegmentDoesNotCountAsAGap() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text and an unknown-type segment, a text and null, a text and a typeless segment, a text and a gap without answer, and 25 gaps followed by an unknown-type segment
		var unknownType = addRaw(setup, raw(TEXT_HI, "{\"type\": \"image\"}"));
		var nullSegment = addRaw(setup, raw(TEXT_HI, "null"));
		var typeless = addRaw(setup, raw(TEXT_HI, "{\"answer\": \"go\"}"));
		var gapWithoutAnswer = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\"}"));
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
		// and the gap without an answer has only its own error, so no payload.segments error
		expectValidation(gapWithoutAnswer, error("payload.segments[1].answer", "REQUIRED"));
		// and the last has only the error of segment 25, with no TOO_LONG
		expectValidation(gapsThenImage, error("payload.segments[25].type", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
