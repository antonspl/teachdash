package com.teachdash.items.api;

import static com.teachdash.items.support.ChoicePayloads.EMOJI;
import static com.teachdash.items.support.ChoicePayloads.json;
import static com.teachdash.items.support.ChoicePayloads.numberedOptions;
import static com.teachdash.items.support.ChoicePayloads.option;
import static com.teachdash.items.support.ChoicePayloads.optionsOf;
import static com.teachdash.items.support.ChoicePayloads.payload;
import static com.teachdash.items.support.ChoicePayloads.question;
import static com.teachdash.items.support.ChoicePayloads.questionWith;
import static com.teachdash.items.support.ChoicePayloads.questions;
import static com.teachdash.items.support.ChoicePayloads.rawQuestion;
import static com.teachdash.items.support.ChoicePayloads.rawQuestions;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-test R2. */
class ChoicePayloadTest extends ItemApiTest {

	private static final String OPTION_A = "{\"text\": \"A\", \"correct\": true}";

	private static final String OPTION_B = "{\"text\": \"B\", \"correct\": false}";

	private static final String VALID_OPTIONS = "[" + OPTION_A + ", " + OPTION_B + "]";

	private static final String VALID_QUESTION = rawQuestion("\"Q\"", VALID_OPTIONS);

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("test", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("test", payloadJson));
	}

	/** A payload with one question "Q" and the given raw JSON as its options. */
	private Response addOptions(Setup setup, String optionsJson) {
		return addRaw(setup, rawQuestions(rawQuestion("\"Q\"", optionsJson)));
	}

	/** A payload with one question with the given raw JSON of its text and the valid options "A" and "B". */
	private Response addText(Setup setup, String textJson) {
		return addRaw(setup, rawQuestions("{" + textJson + ", \"options\": " + VALID_OPTIONS + "}"));
	}

	/** A payload with one question "Q" whose second option is the given raw JSON. */
	private Response addSecondOption(Setup setup, String optionJson) {
		return addOptions(setup, "[" + OPTION_A + ", " + optionJson + "]");
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
	@DisplayName("[item-test/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 25 questions, 500 Cyrillic letters and 10 options, emoji at the limits, and 100-letter options (multiple true) with a Russian question
		var questions25 = payload(questions(25));
		var longQuestion = payload(question("Я".repeat(500), numberedOptions(10)));
		var emojiLimits = payload(question(
				EMOJI.repeat(250),
				option("A", true),
				option(EMOJI.repeat(50), false)));
		var longOptions = payload(
				questionWith(
						"Q",
						true,
						option("a".repeat(100), true),
						option("b".repeat(100), true)),
				question(
						"Вопрос",
						option("да", true),
						option("нет", false)));
		var responses = List.of(
				add(setup, questions25),
				add(setup, longQuestion),
				add(setup, emojiLimits),
				add(setup, longOptions));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), questions25, 1);
		assertCreated(responses.get(1), longQuestion, 2);
		assertCreated(responses.get(2), emojiLimits, 3);
		assertCreated(responses.get(3), longOptions, 4);
	}

	@Test
	@DisplayName("[item-test/R2-S2] A question, an option, a question list or an option list over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with each of the six over-limit payloads
		var longQuestion = add(setup, payload(question("Я".repeat(501), option("A", true), option("B", false))));
		var emojiQuestion = add(setup, payload(question(EMOJI.repeat(251), option("A", true), option("B", false))));
		var longOption = add(setup, payload(question("Q", option("A", true), option("a".repeat(101), false))));
		var emojiOption = add(setup, payload(question("Q", option("A", true), option(EMOJI.repeat(51), false))));
		var elevenOptions = add(setup, payload(question("Q", numberedOptions(11))));
		var questions26 = add(setup, payload(questions(26)));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG errors of the scenario
		expectValidation(longQuestion, error("payload.questions[0].text", "TOO_LONG"));
		expectValidation(emojiQuestion, error("payload.questions[0].text", "TOO_LONG"));
		expectValidation(longOption, error("payload.questions[0].options[1].text", "TOO_LONG"));
		expectValidation(emojiOption, error("payload.questions[0].options[1].text", "TOO_LONG"));
		expectValidation(elevenOptions, error("payload.questions[0].options", "TOO_LONG"));
		expectValidation(questions26, error("payload.questions", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(longQuestion, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(emojiQuestion, EMOJI.repeat(5));
		assertNoEcho(longOption, "aaaaaaaaaa");
		assertNoEcho(emojiOption, EMOJI.repeat(5));
		assertNoEcho(elevenOptions, "o11");
		assertNoEcho(questions26);
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S3] Missing, null or empty questions and a null question are required")
	void missingNullOrEmptyQuestionsAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the payloads {}, {"questions": null}, {"questions": []} and a null second question
		var missing = addRaw(setup, "{}");
		var nullQuestions = addRaw(setup, "{\"questions\": null}");
		var emptyQuestions = addRaw(setup, "{\"questions\": []}");
		var nullQuestion = addRaw(setup, rawQuestions(VALID_QUESTION, "null"));

		// then the first three have payload.questions REQUIRED
		for (Response response : List.of(missing, nullQuestions, emptyQuestions)) {
			expectValidation(response, error("payload.questions", "REQUIRED"));
		}
		// and the last has payload.questions[1] REQUIRED
		expectValidation(nullQuestion, error("payload.questions[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S4] A missing, null, empty or blank question text is required")
	void aMissingNullEmptyOrBlankQuestionTextIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a question whose text is missing, null, "", "   " and "\n\t"
		var missing = addRaw(setup, rawQuestions("{\"options\": " + VALID_OPTIONS + "}"));
		var nullText = addText(setup, "\"text\": null");
		var empty = addText(setup, "\"text\": \"\"");
		var spaces = addText(setup, "\"text\": \"   \"");
		var whitespace = addText(setup, "\"text\": \"\\n\\t\"");

		// then all five responses have payload.questions[0].text REQUIRED
		for (Response response : List.of(missing, nullText, empty, spaces, whitespace)) {
			expectValidation(response, error("payload.questions[0].text", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S5] Options need at least two entries, and a null option is required")
	void optionsNeedAtLeastTwoEntries() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a question whose options are missing, null, [], one option and a null in the middle
		var missing = addRaw(setup, rawQuestions("{\"text\": \"Q\"}"));
		var nullOptions = addOptions(setup, "null");
		var emptyOptions = addOptions(setup, "[]");
		var oneOption = addOptions(setup, "[" + OPTION_A + "]");
		var nullOption = addOptions(setup, "[" + OPTION_A + ", null, " + OPTION_B + "]");

		// then the first three have payload.questions[0].options REQUIRED
		for (Response response : List.of(missing, nullOptions, emptyOptions)) {
			expectValidation(response, error("payload.questions[0].options", "REQUIRED"));
		}
		// and one option is TOO_SHORT
		expectValidation(oneOption, error("payload.questions[0].options", "TOO_SHORT"));
		// and the null option is REQUIRED at payload.questions[0].options[1]
		expectValidation(nullOption, error("payload.questions[0].options[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S6] An option needs a non-blank text and a correct value")
	void anOptionNeedsATextAndACorrectValue() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items whose second option is {"correct": false}, a null, empty and blank text, no correct and a null correct
		var missingText = addSecondOption(setup, "{\"correct\": false}");
		var nullText = addSecondOption(setup, "{\"text\": null, \"correct\": false}");
		var emptyText = addSecondOption(setup, "{\"text\": \"\", \"correct\": false}");
		var blankText = addSecondOption(setup, "{\"text\": \"  \", \"correct\": false}");
		var missingCorrect = addSecondOption(setup, "{\"text\": \"B\"}");
		var nullCorrect = addSecondOption(setup, "{\"text\": \"B\", \"correct\": null}");

		// then the first four have payload.questions[0].options[1].text REQUIRED
		for (Response response : List.of(missingText, nullText, emptyText, blankText)) {
			expectValidation(response, error("payload.questions[0].options[1].text", "REQUIRED"));
		}
		// and the last two have payload.questions[0].options[1].correct REQUIRED
		for (Response response : List.of(missingCorrect, nullCorrect)) {
			expectValidation(response, error("payload.questions[0].options[1].correct", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S7] A repeated option text in a question is rejected only on an exact match")
	void aRepeatedOptionTextIsRejectedOnlyOnAnExactMatch() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the options [go, walk, go], [Ёлка, ёлка, елка, Ёлка, Ёлка] and two questions with distinct texts
		var repeated = add(setup, payload(question("Q", optionsOf("go", "walk", "go"))));
		var repeatedCyrillic = add(setup, payload(question("Q", optionsOf("Ёлка", "ёлка", "елка", "Ёлка", "Ёлка"))));
		var distinct = payload(
				question("Q1", optionsOf("go", "Go", " go", "go ")),
				question("Q2", optionsOf("go", "walk")));
		var accepted = add(setup, distinct);

		// then the first is 400 with INVALID_FORMAT on the later option
		expectValidation(repeated, error("payload.questions[0].options[2].text", "INVALID_FORMAT"));
		// and the second has INVALID_FORMAT on each later copy
		expectValidation(
				repeatedCyrillic,
				error("payload.questions[0].options[3].text", "INVALID_FORMAT"),
				error("payload.questions[0].options[4].text", "INVALID_FORMAT"));
		// and the third is 201 with all options exactly as sent
		assertCreated(accepted, distinct, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S8] A value of the wrong JSON type is rejected with its path")
	void aValueOfTheWrongJsonTypeIsRejectedWithItsPath() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the wrongly typed payloads of the scenario
		var questionsString = addRaw(setup, "{\"questions\": \"Q\"}");
		var questionsObject = addRaw(setup, "{\"questions\": {}}");
		var questionString = addRaw(setup, "{\"questions\": [\"Q\"]}");
		var textNumber = addText(setup, "\"text\": 5");
		var textArray = addText(setup, "\"text\": [\"Привет\"]");
		var optionsString = addOptions(setup, "\"A\"");
		var optionString = addOptions(setup, "[\"A\", \"B\"]");
		var correctString = addSecondOption(setup, "{\"text\": \"B\", \"correct\": \"yes\"}");
		var correctArray = addSecondOption(setup, "{\"text\": \"B\", \"correct\": []}");

		// then every response is 400 VALIDATION_FAILED with INVALID_TYPE at its own path
		expectValidation(questionsString, error("payload.questions", "INVALID_TYPE"));
		expectValidation(questionsObject, error("payload.questions", "INVALID_TYPE"));
		expectValidation(questionString, error("payload.questions[0]", "INVALID_TYPE"));
		expectValidation(textNumber, error("payload.questions[0].text", "INVALID_TYPE"));
		expectValidation(textArray, error("payload.questions[0].text", "INVALID_TYPE"));
		expectValidation(optionsString, error("payload.questions[0].options", "INVALID_TYPE"));
		expectValidation(optionString, error("payload.questions[0].options[0]", "INVALID_TYPE"));
		expectValidation(correctString, error("payload.questions[0].options[1].correct", "INVALID_TYPE"));
		expectValidation(correctArray, error("payload.questions[0].options[1].correct", "INVALID_TYPE"));
		// and no body contains "Привет", and the part has no item
		assertNoEcho(textArray, "Привет");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S9] An unknown property is rejected without echoing its name")
	void anUnknownPropertyIsRejectedWithoutEchoingItsName() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a valid payload that also has an unknown property on the payload, a question, an option and a question
		var timer = addRaw(setup, "{\"timer\": 60, \"questions\": [" + VALID_QUESTION + "]}");
		var shuffle = addRaw(setup, rawQuestions(
				"{\"text\": \"Q\", \"shuffle\": true, \"options\": " + VALID_OPTIONS + "}"));
		var optionId = addOptions(setup, "["
				+ "{\"text\": \"A\", \"correct\": true, \"id\": \"opt-7731\"}, " + OPTION_B + "]");
		var points = addRaw(setup, rawQuestions(
				"{\"text\": \"Q\", \"points\": 2, \"options\": " + VALID_OPTIONS + "}"));

		// then all four responses are 400 with payload UNKNOWN_FIELD, and the name is not echoed
		for (Response response : List.of(timer, shuffle, optionId, points)) {
			expectValidation(response, error("payload", "UNKNOWN_FIELD"));
		}
		assertNoEcho(timer, "timer");
		assertNoEcho(shuffle, "shuffle");
		assertNoEcho(optionId, "opt-7731");
		assertNoEcho(points, "points");
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S10] A NUL in an option and a lone surrogate in a question are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejectedAsPayload() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in an option and a lone high surrogate in a question text, written as JSON escapes
		var nul = addOptions(setup, "[{\"text\": \"a\\u0000b\", \"correct\": true}, " + OPTION_B + "]");
		var surrogate = addText(setup, "\"text\": \"a\\ud800b\"");

		// then both responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT
		expectValidation(nul, error("payload", "INVALID_FORMAT"));
		expectValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value, and the part has no item
		assertNoEcho(nul, "a\u0000b", "a\\u0000b");
		assertNoEcho(surrogate, "a\ud800b", "a\\ud800b");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S11] Limits are checked first, the structure rules after, and all structure errors come together")
	void limitsAreCheckedFirstAndStructureErrorsComeTogether() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the four payloads of the scenario
		var longTextAndRepeat = add(setup, payload(question("a".repeat(501), optionsOf("A", "A"))));
		var manyStructureErrors = addRaw(setup, rawQuestions(
				"{\"text\": \"Q1\", \"multiple\": true, \"options\": [" + OPTION_A + ", " + OPTION_A + ", null]}",
				"null"));
		var onlyRepeat = add(setup, payload(questionWith("Q", true, option("A", true), option("A", true))));
		var limitAndRequired = addSecondOption(
				setup,
				"{\"text\": \"" + "a".repeat(101) + "\", \"correct\": null}");

		// then (a) has only TOO_LONG on the text, the duplicate not yet reported
		expectValidation(longTextAndRepeat, error("payload.questions[0].text", "TOO_LONG"));
		// and (b) has the three structure errors sorted by field
		expectValidation(
				manyStructureErrors,
				error("payload.questions[0].options[1].text", "INVALID_FORMAT"),
				error("payload.questions[0].options[2]", "REQUIRED"),
				error("payload.questions[1]", "REQUIRED"));
		// and (c) with multiple true has the repeat only
		expectValidation(onlyRepeat, error("payload.questions[0].options[1].text", "INVALID_FORMAT"));
		// and (d) has the two Bean Validation errors sorted by field
		expectValidation(
				limitAndRequired,
				error("payload.questions[0].options[1].correct", "REQUIRED"),
				error("payload.questions[0].options[1].text", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S12] `correct` accepts only a JSON boolean")
	void correctAcceptsOnlyAJsonBoolean() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items whose second option has "correct" set to each non-boolean value and to null
		List<String> notBooleans = List.of("\"true\"", "\"TRUE\"", "\"false\"", "1", "0", "2", "1.0", "\"\"");
		List<Response> wrongTypes = notBooleans.stream()
				.map(value -> addSecondOption(setup, "{\"text\": \"B\", \"correct\": " + value + "}"))
				.toList();
		var nullCorrect = addSecondOption(setup, "{\"text\": \"B\", \"correct\": null}");

		// then the eight responses are 400 with payload.questions[0].options[1].correct INVALID_TYPE
		for (Response response : wrongTypes) {
			expectValidation(response, error("payload.questions[0].options[1].correct", "INVALID_TYPE"));
		}
		// and null is REQUIRED at the same field
		expectValidation(nullCorrect, error("payload.questions[0].options[1].correct", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S13] `multiple` accepts only a JSON boolean, and a null value is dropped")
	void multipleAcceptsOnlyAJsonBoolean() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with one question "Q" and multiple set to each non-boolean value
		List<String> notBooleans = List.of("\"true\"", "\"TRUE\"", "\"false\"", "1", "0", "2", "1.0", "\"\"", "[]", "{}");
		List<Response> wrongTypes = notBooleans.stream()
				.map(value -> addRaw(setup, rawQuestions(
						"{\"text\": \"Q\", \"multiple\": " + value + ", \"options\": " + VALID_OPTIONS + "}")))
				.toList();

		// and then with multiple true, false, null and no multiple
		var withTrue = payload(questionWith("Q", true, option("A", true), option("B", false)));
		var withFalse = payload(questionWith("Q", false, option("A", true), option("B", false)));
		var withNull = payload(questionWith("Q", null, option("A", true), option("B", false)));
		var withNone = payload(question("Q", option("A", true), option("B", false)));
		var acceptedTrue = add(setup, withTrue);
		var acceptedFalse = add(setup, withFalse);
		var acceptedNull = add(setup, withNull);
		var acceptedNone = add(setup, withNone);

		// then the responses for the first ten values are 400 with payload.questions[0].multiple INVALID_TYPE
		for (Response response : wrongTypes) {
			expectValidation(response, error("payload.questions[0].multiple", "INVALID_TYPE"));
		}
		// and true and false come back exactly as sent
		assertCreated(acceptedTrue, withTrue, 1);
		assertCreated(acceptedFalse, withFalse, 2);
		// and the null case and the case with no multiple come back with no multiple key
		assertCreated(acceptedNull, withNone, 3);
		assertCreated(acceptedNone, withNone, 4);
		acceptedNull.then().body("payload.questions[0]", not(hasKey("multiple")));
		acceptedNone.then().body("payload.questions[0]", not(hasKey("multiple")));
		// and the part has exactly the last four items
		assertEquals(4, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-test/R2-S14] More than one correct option needs `multiple` true")
	void moreThanOneCorrectOptionNeedsMultipleTrue() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with two correct options and no multiple, multiple false and multiple null
		var noMultiple = add(setup, payload(question("Q", option("A", true), option("B", true), option("C", false))));
		var multipleFalse = add(setup, payload(
				questionWith("Q", false, option("A", true), option("B", true), option("C", false))));
		var multipleNull = add(setup, payload(
				questionWith("Q", null, option("A", true), option("B", true), option("C", false))));

		// and with the options "A" and "A", both correct, and no multiple
		var repeated = add(setup, payload(question("Q", option("A", true), option("A", true))));

		// and with the same question and multiple true
		var multipleTrue = payload(questionWith("Q", true, option("A", true), option("B", true), option("C", false)));
		var acceptedTrue = add(setup, multipleTrue);

		// and with multiple true and no correct option, multiple true and all correct, multiple false and no correct, no multiple and one correct
		var trueNoCorrect = payload(questionWith("Q", true, option("A", false), option("B", false), option("C", false)));
		var trueAllCorrect = payload(questionWith("Q", true, option("A", true), option("B", true), option("C", true)));
		var falseNoCorrect = payload(questionWith("Q", false, option("A", false), option("B", false), option("C", false)));
		var noneOneCorrect = payload(question("Q", option("A", true), option("B", false), option("C", false)));
		var acceptedOthers = List.of(
				add(setup, trueNoCorrect),
				add(setup, trueAllCorrect),
				add(setup, falseNoCorrect),
				add(setup, noneOneCorrect));

		// then the first three are 400 VALIDATION_FAILED with payload.questions[0].options INVALID_FORMAT
		for (Response response : List.of(noMultiple, multipleFalse, multipleNull)) {
			expectValidation(response, error("payload.questions[0].options", "INVALID_FORMAT"));
		}
		// and the repeated options have both structure errors, sorted by field
		expectValidation(
				repeated,
				error("payload.questions[0].options", "INVALID_FORMAT"),
				error("payload.questions[0].options[1].text", "INVALID_FORMAT"));
		// and multiple true and the four other payloads are 201 with the payload as sent
		assertCreated(acceptedTrue, multipleTrue, 1);
		assertCreated(acceptedOthers.get(0), trueNoCorrect, 2);
		assertCreated(acceptedOthers.get(1), trueAllCorrect, 3);
		assertCreated(acceptedOthers.get(2), falseNoCorrect, 4);
		assertCreated(acceptedOthers.get(3), noneOneCorrect, 5);
		// and the part has exactly five items
		assertEquals(5, countItems(setup.part()));
	}
}
