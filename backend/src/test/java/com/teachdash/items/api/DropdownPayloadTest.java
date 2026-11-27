package com.teachdash.items.api;

import static com.teachdash.items.support.DropdownPayloads.EMOJI;
import static com.teachdash.items.support.DropdownPayloads.alternating;
import static com.teachdash.items.support.DropdownPayloads.gap;
import static com.teachdash.items.support.DropdownPayloads.gaps;
import static com.teachdash.items.support.DropdownPayloads.json;
import static com.teachdash.items.support.DropdownPayloads.payload;
import static com.teachdash.items.support.DropdownPayloads.rawSegments;
import static com.teachdash.items.support.DropdownPayloads.text;
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

/** item-dropdown R2. */
class DropdownPayloadTest extends ItemApiTest {

	private static final String GAP_AB = "{\"type\": \"gap\", \"options\": [\"a\", \"b\"], \"correct\": 0}";

	private static final String TEXT_A = "{\"type\": \"text\", \"text\": \"A\"}";

	private static final String TEXT_HI = "{\"type\": \"text\", \"text\": \"Hi\"}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("dropdown", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("dropdown", payloadJson));
	}

	private static String raw(String... segments) {
		return rawSegments(segments);
	}

	private static String gapJson(int correct, String... options) {
		List<String> quoted = new ArrayList<>();
		for (String option : options) {
			quoted.add(option == null ? "null" : "\"" + option + "\"");
		}
		return "{\"type\": \"gap\", \"options\": [" + String.join(", ", quoted) + "], \"correct\": " + correct + "}";
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
	@DisplayName("[item-dropdown/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 51 segments, 5 options of 100 Cyrillic letters, a 500-letter text with a 50-emoji option, and a 250-emoji text
		var segments51 = payload(alternating(51, "a", 500, true));
		List<String> options = new ArrayList<>();
		for (String last : List.of("а", "б", "в", "г", "д")) {
			options.add("Я".repeat(99) + last);
		}
		var fiveOptions = payload(gap(4, options.toArray(String[]::new)));
		var longCyrillic = payload(text("Я".repeat(500)), gap(0, "x", EMOJI.repeat(50)));
		var longEmojiText = payload(text(EMOJI.repeat(250)), gap(0, "go", "went"));
		var responses = List.of(
				add(setup, segments51),
				add(setup, fiveOptions),
				add(setup, longCyrillic),
				add(setup, longEmojiText));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), segments51, 1);
		assertCreated(responses.get(1), fiveOptions, 2);
		assertCreated(responses.get(2), longCyrillic, 3);
		assertCreated(responses.get(3), longEmojiText, 4);
	}

	@Test
	@DisplayName("[item-dropdown/R2-S2] A text, an option, an option list, a segment list or a gap count over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a 501-letter text, a 251-emoji text, a 101-letter option, a 51-emoji option, 6 options, 52 segments and 26 adjacent gaps
		var longText = add(setup, payload(text("Я".repeat(501)), gap(0, "go", "went")));
		var longEmojiText = add(setup, payload(text(EMOJI.repeat(251)), gap(0, "go", "went")));
		var longOption = add(setup, payload(gap(0, "a".repeat(101), "b")));
		var longEmojiOption = add(setup, payload(gap(0, EMOJI.repeat(51), "b")));
		var sixOptions = add(setup, payload(gap(0, "a", "b", "c", "d", "e", "f")));
		var segments52 = add(setup, payload(alternating(52, "a", 5, false)));
		var gaps26 = add(setup, payload(gaps(26)));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG errors of the scenario
		expectValidation(longText, error("payload.segments[0].text", "TOO_LONG"));
		expectValidation(longEmojiText, error("payload.segments[0].text", "TOO_LONG"));
		expectValidation(longOption, error("payload.segments[0].options[0]", "TOO_LONG"));
		expectValidation(longEmojiOption, error("payload.segments[0].options[0]", "TOO_LONG"));
		expectValidation(sixOptions, error("payload.segments[0].options", "TOO_LONG"));
		expectValidation(segments52, error("payload.segments", "TOO_LONG"));
		expectValidation(gaps26, error("payload.segments", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(longText, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmojiText, EMOJI.repeat(5));
		assertNoEcho(longOption, "aaaaaaaaaa");
		assertNoEcho(longEmojiOption, EMOJI.repeat(5));
		assertNoEcho(sixOptions);
		assertNoEcho(segments52);
		assertNoEcho(gaps26);
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S3] A missing or empty text, and a payload without a gap, are required")
	void aMissingOrEmptyTextAndAGaplessPayloadAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text without text, with a null text, with an empty text, with only a text, and with the payloads {"segments": []}, {"segments": null} and {}
		var noText = addRaw(setup, raw(GAP_AB, "{\"type\": \"text\"}"));
		var nullText = addRaw(setup, raw(GAP_AB, "{\"type\": \"text\", \"text\": null}"));
		var emptyText = addRaw(setup, raw(GAP_AB, "{\"type\": \"text\", \"text\": \"\"}"));
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
	@DisplayName("[item-dropdown/R2-S4] A gap needs 2 to 5 options, and every option must be non-blank")
	void aGapNeedsTwoToFiveOptionsAndEveryOptionMustBeNonBlank() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with seven bad gaps, each as the second segment after a text segment
		var noOptions = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"correct\": 0}"));
		var nullOptions = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"options\": null, \"correct\": 0}"));
		var emptyOptions = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"options\": [], \"correct\": 0}"));
		var oneOption = addRaw(setup, raw(TEXT_A, gapJson(0, "a")));
		var emptyOption = addRaw(setup, raw(TEXT_A, gapJson(0, "go", "")));
		var blankOption = addRaw(setup, raw(TEXT_A, gapJson(0, "  ", "go")));
		var nullOption = addRaw(setup, raw(TEXT_A, gapJson(0, "go", null)));

		// then the first two have payload.segments[1].options REQUIRED, with no payload.segments error
		for (Response response : List.of(noOptions, nullOptions)) {
			expectValidation(response, error("payload.segments[1].options", "REQUIRED"));
		}
		// and the next two have payload.segments[1].options TOO_SHORT
		for (Response response : List.of(emptyOptions, oneOption)) {
			expectValidation(response, error("payload.segments[1].options", "TOO_SHORT"));
		}
		// and the last three have REQUIRED on the option at index 1, 0 and 1
		expectValidation(emptyOption, error("payload.segments[1].options[1]", "REQUIRED"));
		expectValidation(blankOption, error("payload.segments[1].options[0]", "REQUIRED"));
		expectValidation(nullOption, error("payload.segments[1].options[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S5] A repeated option in a gap is rejected only on an exact match")
	void aRepeatedOptionIsRejectedOnlyOnAnExactMatch() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the options [go, walk, go], [Ёлка, ёлка, елка, Ёлка], and [go, Go, " go", "go "] with [go, stop] in another gap
		var repeated = add(setup, payload(gap(0, "go", "walk", "go")));
		var repeatedCyrillic = add(setup, payload(gap(0, "Ёлка", "ёлка", "елка", "Ёлка")));
		var distinct = payload(gap(3, "go", "Go", " go", "go "), gap(0, "go", "stop"));
		var accepted = add(setup, distinct);

		// then the first two are 400 with INVALID_FORMAT on the later option
		expectValidation(repeated, error("payload.segments[0].options[2]", "INVALID_FORMAT"));
		expectValidation(repeatedCyrillic, error("payload.segments[0].options[3]", "INVALID_FORMAT"));
		// and the third is 201 with all options and correct values exactly as sent
		assertCreated(accepted, distinct, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S6] Two adjacent text segments are rejected on the second")
	void twoAdjacentTextSegmentsAreRejectedOnTheSecond() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the segments [text, text, gap] and [gap, text, text, text]
		var firstPair = add(setup, payload(text("A"), text("B"), gap(0, "a", "b")));
		var tripleText = add(setup, payload(gap(0, "a", "b"), text("A"), text("B"), text("C")));

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
	@DisplayName("[item-dropdown/R2-S7] A field of the other segment type, or an unknown property, is rejected")
	void aFieldOfTheOtherSegmentTypeOrAnUnknownPropertyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds the seven payloads (a) to (g) of the scenario
		var gapWithText = addRaw(setup, raw(
				TEXT_HI,
				"{\"type\": \"gap\", \"text\": \"go\", \"options\": [\"a\", \"b\"], \"correct\": 0}"));
		var textWithOptions = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"options\": [\"a\", \"b\"]}", GAP_AB));
		var textWithCorrect = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"correct\": 0}", GAP_AB));
		var unknownInSegment = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"hint\": \"x\"}", GAP_AB));
		var unknownInPayload = addRaw(setup, "{\"segments\": [" + TEXT_HI + ", " + GAP_AB + "], \"caption\": \"Intro\"}");
		var gapWithId = addRaw(setup, raw(
				TEXT_HI,
				"{\"type\": \"gap\", \"id\": \"seg-7731\", \"options\": [\"a\", \"b\"], \"correct\": 0}"));
		var textWithShortOptions = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"Hi\", \"options\": [\"a\"]}", GAP_AB));

		// then (a) is UNKNOWN_FIELD at payload.segments[1].text
		expectValidation(gapWithText, error("payload.segments[1].text", "UNKNOWN_FIELD"));
		// and (b) and (c) are UNKNOWN_FIELD at the known property of the text segment
		expectValidation(textWithOptions, error("payload.segments[0].options", "UNKNOWN_FIELD"));
		expectValidation(textWithCorrect, error("payload.segments[0].correct", "UNKNOWN_FIELD"));
		// and (d), (e) and (f) are UNKNOWN_FIELD at payload, with no name
		for (Response response : List.of(unknownInSegment, unknownInPayload, gapWithId)) {
			expectValidation(response, error("payload", "UNKNOWN_FIELD"));
		}
		assertNoEcho(unknownInSegment, "hint");
		assertNoEcho(unknownInPayload, "caption");
		assertNoEcho(unknownInPayload, "Intro");
		assertNoEcho(gapWithId, "seg-7731");
		// and (g) is TOO_SHORT at payload.segments[0].options, because Bean Validation runs first
		expectValidation(textWithShortOptions, error("payload.segments[0].options", "TOO_SHORT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S8] A missing, blank or unknown segment type and a null segment are rejected")
	void aBadSegmentTypeAndANullSegmentAreRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a bad segment after a valid gap: no type, a null type, a blank type, "image", "Gap" and null
		var noType = addRaw(setup, raw(GAP_AB, "{\"text\": \"Hi\"}"));
		var nullType = addRaw(setup, raw(GAP_AB, "{\"type\": null, \"text\": \"Hi\"}"));
		var blankType = addRaw(setup, raw(GAP_AB, "{\"type\": \" \", \"text\": \"Hi\"}"));
		var image = addRaw(setup, raw(GAP_AB, "{\"type\": \"image\", \"text\": \"Hi\"}"));
		var capitalised = addRaw(setup, raw(GAP_AB, "{\"type\": \"Gap\", \"options\": [\"a\", \"b\"], \"correct\": 0}"));
		var nullSegment = addRaw(setup, raw(GAP_AB, "null"));

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
	@DisplayName("[item-dropdown/R2-S9] A value of the wrong JSON type is rejected with its path")
	void aValueOfTheWrongJsonTypeIsRejectedWithItsPath() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the ten wrongly typed payloads of the scenario
		var segmentsString = addRaw(setup, "{\"segments\": \"text\"}");
		var segmentsObject = addRaw(setup, "{\"segments\": {}}");
		var segmentString = addRaw(setup, "{\"segments\": [\"text\"]}");
		var typeNumber = addRaw(setup, "{\"segments\": [{\"type\": 5, \"text\": \"Hi\"}]}");
		var textArray = addRaw(setup, "{\"segments\": [{\"type\": \"text\", \"text\": [\"Привет\"]}]}");
		var optionsString = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"options\": \"go\", \"correct\": 0}]}");
		var optionNumber = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"options\": [\"go\", 5], \"correct\": 0}]}");
		var correctString = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"options\": [\"a\", \"b\"], \"correct\": \"1\"}]}");
		var correctBoolean = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"options\": [\"a\", \"b\"], \"correct\": true}]}");
		var correctFraction = addRaw(setup, "{\"segments\": [{\"type\": \"gap\", \"options\": [\"a\", \"b\"], \"correct\": 1.5}]}");

		// then every response is 400 VALIDATION_FAILED with INVALID_TYPE at its own path
		expectValidation(segmentsString, error("payload.segments", "INVALID_TYPE"));
		expectValidation(segmentsObject, error("payload.segments", "INVALID_TYPE"));
		expectValidation(segmentString, error("payload.segments[0]", "INVALID_TYPE"));
		expectValidation(typeNumber, error("payload.segments[0].type", "INVALID_TYPE"));
		expectValidation(textArray, error("payload.segments[0].text", "INVALID_TYPE"));
		expectValidation(optionsString, error("payload.segments[0].options", "INVALID_TYPE"));
		expectValidation(optionNumber, error("payload.segments[0].options[1]", "INVALID_TYPE"));
		for (Response response : List.of(correctString, correctBoolean, correctFraction)) {
			expectValidation(response, error("payload.segments[0].correct", "INVALID_TYPE"));
		}
		// and no body contains "Привет", and the part has no item
		assertNoEcho(textArray, "Привет");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S10] `correct` is required and must point at an option")
	void correctIsRequiredAndMustPointAtAnOption() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the nine gaps (a) to (i), each as the second segment after a text segment
		var missing = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"options\": [\"a\", \"b\"]}"));
		var nullCorrect = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"options\": [\"a\", \"b\"], \"correct\": null}"));
		var negative = addRaw(setup, raw(TEXT_A, gapJson(-1, "a", "b")));
		var tooBig = addRaw(setup, raw(TEXT_A, gapJson(2, "a", "b")));
		var duplicateAndFar = addRaw(setup, raw(TEXT_A, gapJson(7, "go", "go")));
		var noOptionsFar = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"correct\": 7}"));
		var emptyOptionFar = addRaw(setup, raw(TEXT_A, gapJson(7, "", "x")));
		var neither = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\"}"));
		var duplicateNoCorrect = addRaw(setup, raw(TEXT_A, "{\"type\": \"gap\", \"options\": [\"go\", \"go\"]}"));

		// then (a) and (b) are REQUIRED at correct
		for (Response response : List.of(missing, nullCorrect)) {
			expectValidation(response, error("payload.segments[1].correct", "REQUIRED"));
		}
		// and (c) and (d) are INVALID_FORMAT at correct
		for (Response response : List.of(negative, tooBig)) {
			expectValidation(response, error("payload.segments[1].correct", "INVALID_FORMAT"));
		}
		// and (e) has only the option error, because the range check is skipped when options has an error
		expectValidation(duplicateAndFar, error("payload.segments[1].options[1]", "INVALID_FORMAT"));
		// and (f) and (g) have only the options error
		expectValidation(noOptionsFar, error("payload.segments[1].options", "REQUIRED"));
		expectValidation(emptyOptionFar, error("payload.segments[1].options[0]", "REQUIRED"));
		// and (h) and (i) have both errors, sorted by field
		expectValidation(
				neither,
				error("payload.segments[1].correct", "REQUIRED"),
				error("payload.segments[1].options", "REQUIRED"));
		expectValidation(
				duplicateNoCorrect,
				error("payload.segments[1].correct", "REQUIRED"),
				error("payload.segments[1].options[1]", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S11] Limits are checked first, the structure rules after, and all structure errors come together")
	void limitsAreCheckedFirstAndStructureErrorsComeTogether() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds the five payloads of the scenario
		var longTextAndRepeat = add(setup, payload(text("a".repeat(501)), gap(0, "go", "go")));
		var manyStructureErrors = add(setup, payload(
				text("A"), text("B"), gap(5, "go", "go"), gap(0, "", "x"), gap(9, "a", "b")));
		var onlyRepeat = add(setup, payload(text("A"), gap(0, "go", "go")));
		var gapWithLongText = addRaw(setup, raw(
				TEXT_A,
				"{\"type\": \"gap\", \"options\": [\"a\", \"b\"], \"correct\": 0, \"text\": \"" + "a".repeat(501) + "\"}"));
		var sixOptionsAndLongOption = add(setup, payload(gap(0, "a".repeat(101), "b", "c", "d", "e", "f")));

		// then the first has only TOO_LONG on the text, the duplicate not yet reported
		expectValidation(longTextAndRepeat, error("payload.segments[0].text", "TOO_LONG"));
		// and the second has the four structure errors sorted by field
		expectValidation(
				manyStructureErrors,
				error("payload.segments[1]", "INVALID_FORMAT"),
				error("payload.segments[2].options[1]", "INVALID_FORMAT"),
				error("payload.segments[3].options[0]", "REQUIRED"),
				error("payload.segments[4].correct", "INVALID_FORMAT"));
		// and the third has the repeat only
		expectValidation(onlyRepeat, error("payload.segments[1].options[1]", "INVALID_FORMAT"));
		// and the fourth has TOO_LONG, not UNKNOWN_FIELD, because Bean Validation runs first
		expectValidation(gapWithLongText, error("payload.segments[1].text", "TOO_LONG"));
		// and the fifth has TOO_LONG on options only, the over-long option not yet reported
		expectValidation(sixOptionsAndLongOption, error("payload.segments[0].options", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S12] Whitespace-only text between gaps is accepted")
	void whitespaceOnlyTextBetweenGapsIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text " " between two gaps, and with a line break between gaps and a whitespace-only text last
		var space = payload(text("I "), gap(0, "go", "went"), text(" "), gap(1, "walk", "ran"), text(" to school."));
		var lineBreak = payload(gap(0, "go", "went"), text("\n"), gap(0, "walk", "ran"), text("  \n\t"));
		var first = add(setup, space);
		var second = add(setup, lineBreak);

		// then both responses are 201 with the segments exactly as sent, in the positions 1 and 2
		assertCreated(first, space, 1);
		assertCreated(second, lineBreak, 2);
	}

	@Test
	@DisplayName("[item-dropdown/R2-S13] A NUL in an option and a lone surrogate in a text are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejectedAsPayload() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in an option and a lone high surrogate in a text, written as JSON escapes
		var nul = addRaw(setup, raw(
				TEXT_A, "{\"type\": \"gap\", \"options\": [\"a\\u0000b\", \"x\"], \"correct\": 0}"));
		var surrogate = addRaw(setup, raw("{\"type\": \"text\", \"text\": \"a\\ud800b\"}", gapJson(0, "go", "went")));

		// then both responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT
		expectValidation(nul, error("payload", "INVALID_FORMAT"));
		expectValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value, and the part has no item
		assertNoEcho(nul, "a\u0000b", "a\\u0000b");
		assertNoEcho(surrogate, "a\ud800b", "a\\ud800b");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-dropdown/R2-S14] A null, typeless or unknown-type segment does not count as a gap, a gap with missing fields does")
	void aNullTypelessOrUnknownTypeSegmentDoesNotCountAsAGap() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a text and an unknown-type segment, a text and null, a text and a typeless segment, a text and a bare gap, and 25 gaps followed by an unknown-type segment
		var unknownType = addRaw(setup, raw(TEXT_HI, "{\"type\": \"image\"}"));
		var nullSegment = addRaw(setup, raw(TEXT_HI, "null"));
		var typeless = addRaw(setup, raw(TEXT_HI, "{\"options\": [\"a\", \"b\"], \"correct\": 0}"));
		var bareGap = addRaw(setup, raw(TEXT_HI, "{\"type\": \"gap\"}"));
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
		// and the bare gap has the errors of its missing fields and no payload.segments error
		expectValidation(
				bareGap,
				error("payload.segments[1].correct", "REQUIRED"),
				error("payload.segments[1].options", "REQUIRED"));
		// and the last has only the error of segment 25, with no TOO_LONG
		expectValidation(gapsThenImage, error("payload.segments[25].type", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
