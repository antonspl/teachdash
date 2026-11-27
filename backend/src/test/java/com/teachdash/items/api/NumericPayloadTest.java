package com.teachdash.items.api;

import static com.teachdash.items.support.NumericPayloads.EMOJI;
import static com.teachdash.items.support.NumericPayloads.json;
import static com.teachdash.items.support.NumericPayloads.payload;
import static com.teachdash.items.support.NumericPayloads.question;
import static com.teachdash.items.support.NumericPayloads.rawQuestion;
import static com.teachdash.items.support.NumericPayloads.rawQuestions;
import static com.teachdash.items.support.NumericPayloads.validQuestion;
import static com.teachdash.items.support.NumericPayloads.validQuestions;
import static com.teachdash.items.support.NumericPayloads.withAnswer;
import static com.teachdash.items.support.NumericPayloads.withTolerance;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-numeric R2. */
class NumericPayloadTest extends ItemApiTest {

	private static final String VALID_FIELDS = "\"text\": \"Q\", \"answer\": \"1\", \"tolerance\": \"0\"";

	private Response add(Setup setup, Object payload) {
		return addRaw(setup, json(payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("numeric", payloadJson));
	}

	/** Adds one question given as raw JSON. */
	private Response addQuestion(Setup setup, String questionJson) {
		return addRaw(setup, rawQuestions(questionJson));
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

	private static Map<String, Object> questionWithUnit(String unit) {
		return question("Q", "1", "0", unit);
	}

	@Test
	@DisplayName("[item-numeric/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 25 questions, the longest Cyrillic texts and numbers, the longest emoji texts, and two ordinary questions
		var questions25 = payload(validQuestions(25));
		var longest = payload(question(
				"Я".repeat(500), "-999999999999999", "999999999999999.9999999999", "к".repeat(20)));
		var emoji = payload(question(EMOJI.repeat(250), "0.0000000001", "0", EMOJI.repeat(10)));
		var ordinary = payload(
				question("Скорость?", "-0.5", "0.01", "км/ч"),
				question("Speed?", "12", "1"));
		var responses = List.of(
				add(setup, questions25),
				add(setup, longest),
				add(setup, emoji),
				add(setup, ordinary));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), questions25, 1);
		assertCreated(responses.get(1), longest, 2);
		assertCreated(responses.get(2), emoji, 3);
		assertCreated(responses.get(3), ordinary, 4);
	}

	@Test
	@DisplayName("[item-numeric/R2-S2] A text, a unit or the question list over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a 501-letter text, a 251-emoji text, a 21-letter unit, an 11-emoji unit and 26 questions
		var longText = add(setup, payload(question("Я".repeat(501), "1", "0")));
		var longEmojiText = add(setup, payload(question(EMOJI.repeat(251), "1", "0")));
		var longUnit = add(setup, payload(questionWithUnit("a".repeat(21))));
		var longEmojiUnit = add(setup, payload(questionWithUnit(EMOJI.repeat(11))));
		var questions26 = add(setup, payload(validQuestions(26)));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG errors of the scenario
		expectValidation(longText, error("payload.questions[0].text", "TOO_LONG"));
		expectValidation(longEmojiText, error("payload.questions[0].text", "TOO_LONG"));
		expectValidation(longUnit, error("payload.questions[0].unit", "TOO_LONG"));
		expectValidation(longEmojiUnit, error("payload.questions[0].unit", "TOO_LONG"));
		expectValidation(questions26, error("payload.questions", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(longText, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmojiText, EMOJI.repeat(5));
		assertNoEcho(longUnit, "aaaaaaaaaa");
		assertNoEcho(longEmojiUnit, EMOJI.repeat(5));
		assertNoEcho(questions26);
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S3] Missing, null or empty questions and a null question are required")
	void missingNullOrEmptyQuestionsAndANullQuestionAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the payloads {}, {"questions": null}, {"questions": []} and a list ending with null
		var empty = addRaw(setup, "{}");
		var nullQuestions = addRaw(setup, "{\"questions\": null}");
		var noQuestions = addRaw(setup, "{\"questions\": []}");
		var nullQuestion = addRaw(setup, rawQuestions(rawQuestion(VALID_FIELDS), "null"));

		// then the first three have payload.questions REQUIRED
		for (Response response : List.of(empty, nullQuestions, noQuestions)) {
			expectValidation(response, error("payload.questions", "REQUIRED"));
		}
		// and the last has payload.questions[1] REQUIRED
		expectValidation(nullQuestion, error("payload.questions[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S4] A missing, null, empty or blank question text is required")
	void aMissingNullEmptyOrBlankTextIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with one question whose text is missing, null, empty, spaces and line break with tab
		var rest = "\"answer\": \"1\", \"tolerance\": \"0\"";
		List<Response> responses = List.of(
				addQuestion(setup, rawQuestion(rest)),
				addQuestion(setup, rawQuestion("\"text\": null", rest)),
				addQuestion(setup, rawQuestion("\"text\": \"\"", rest)),
				addQuestion(setup, rawQuestion("\"text\": \"   \"", rest)),
				addQuestion(setup, rawQuestion("\"text\": \"\\n\\t\"", rest)));

		// then all five responses are 400 VALIDATION_FAILED with payload.questions[0].text REQUIRED
		assertEquals(5, responses.size());
		for (Response response : responses) {
			expectValidation(response, error("payload.questions[0].text", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S5] A unit may be absent or null, but a present unit must not be blank")
	void aUnitMayBeAbsentButAPresentUnitMustNotBeBlank() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a question that has no unit, a null unit, an empty unit, spaces, a tab and 21 spaces
		var noUnit = addQuestion(setup, rawQuestion(VALID_FIELDS));
		var nullUnit = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": null"));
		var emptyUnit = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": \"\""));
		var spacesUnit = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": \"   \""));
		var tabUnit = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": \"\\t\""));
		var longBlankUnit = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": \"" + " ".repeat(21) + "\""));

		// then the first two are 201 and the stored payload has no unit property
		var expected = payload(validQuestion());
		assertCreated(noUnit, expected, 1);
		assertCreated(nullUnit, expected, 2);
		// and the last four are 400 VALIDATION_FAILED with payload.questions[0].unit REQUIRED
		for (Response response : List.of(emptyUnit, spacesUnit, tabUnit, longBlankUnit)) {
			expectValidation(response, error("payload.questions[0].unit", "REQUIRED"));
		}
		// and the part has exactly two items
		assertEquals(2, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S6] A missing, null, empty or blank answer or tolerance is required")
	void aMissingNullEmptyOrBlankAnswerOrToleranceIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();
		var text = "\"text\": \"Q\"";
		var answer = "\"answer\": \"1\"";
		var tolerance = "\"tolerance\": \"0\"";

		// when teacher A adds items in which the answer is missing, null, empty, blank, and then the tolerance is
		List<Response> answers = List.of(
				addQuestion(setup, rawQuestion(text, tolerance)),
				addQuestion(setup, rawQuestion(text, "\"answer\": null", tolerance)),
				addQuestion(setup, rawQuestion(text, "\"answer\": \"\"", tolerance)),
				addQuestion(setup, rawQuestion(text, "\"answer\": \"   \"", tolerance)));
		List<Response> tolerances = List.of(
				addQuestion(setup, rawQuestion(text, answer)),
				addQuestion(setup, rawQuestion(text, answer, "\"tolerance\": null")),
				addQuestion(setup, rawQuestion(text, answer, "\"tolerance\": \"\"")),
				addQuestion(setup, rawQuestion(text, answer, "\"tolerance\": \"   \"")));

		// then the first four are 400 with payload.questions[0].answer REQUIRED
		for (Response response : answers) {
			expectValidation(response, error("payload.questions[0].answer", "REQUIRED"));
		}
		// and the last four are 400 with payload.questions[0].tolerance REQUIRED
		for (Response response : tolerances) {
			expectValidation(response, error("payload.questions[0].tolerance", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S7] Numbers in the allowed format are accepted and stored as written")
	void numbersInTheAllowedFormatAreStoredAsWritten() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds one item with eight questions whose numbers are in the allowed format
		var sent = payload(
				question("Q1", "0", "0"),
				question("Q2", "-0", "0.00"),
				question("Q3", "007", "007"),
				question("Q4", "1.50", "1.50"),
				question("Q5", "-12.5", "3"),
				question("Q6", "999999999999999", "999999999999999"),
				question("Q7", "0.0000000001", "0.0000000001"),
				question("Q8", "-0.1", "0.1"));
		var response = add(setup, sent);

		// then the response is 201 and the payload equals the sent one character by character, and the stored payload has the same content
		assertCreated(response, sent, 1);
	}

	@Test
	@DisplayName("[item-numeric/R2-S8] An answer off the number format is rejected")
	void anAnswerOffTheNumberFormatIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with one question each, whose answer is one of the bad values of the scenario
		List<String> answers = List.of(
				"1,5", "1e3", "1E-3", "+1", " 1", "1 ", "1 000", ".5", "5.", "1/2", "0x10", "NaN", "Infinity",
				"--1", "1.5.2", "-", "١٢", "1234567890123456", "1.12345678901", "1\n", "abc");
		Map<String, Response> responses = new LinkedHashMap<>();
		for (String answer : answers) {
			responses.put(answer, add(setup, payload(withAnswer(answer))));
		}

		// then every response is 400 VALIDATION_FAILED with payload.questions[0].answer INVALID_FORMAT
		assertEquals(21, responses.size());
		responses.forEach((answer, response) ->
				expectValidation(response, error("payload.questions[0].answer", "INVALID_FORMAT")));
		// and no body contains a sent value (only distinctive values are checked: a short one could appear by chance)
		for (String answer : List.of("1 000", "Infinity", "NaN", "0x10", "1.5.2", "1234567890123456", "1.12345678901", "abc")) {
			assertNoEcho(responses.get(answer), answer);
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S9] A tolerance that is negative or off the number format is rejected")
	void aNegativeOrMalformedToleranceIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with one question each, whose tolerance is one of the bad values of the scenario
		List<String> tolerances = List.of(
				"-1", "-0", "-0.5", "-0.0000000001", "0,5", "1e-2", "+1", " 1", ".5", "1/2",
				"1234567890123456", "0.12345678901");
		List<Response> responses = new ArrayList<>();
		for (String tolerance : tolerances) {
			responses.add(add(setup, payload(withTolerance(tolerance))));
		}

		// then every response is 400 VALIDATION_FAILED with payload.questions[0].tolerance INVALID_FORMAT
		assertEquals(12, responses.size());
		for (Response response : responses) {
			expectValidation(response, error("payload.questions[0].tolerance", "INVALID_FORMAT"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S10] A value of the wrong JSON type is rejected with its path")
	void aValueOfTheWrongJsonTypeIsRejectedWithItsPath() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();
		var text = "\"text\": \"Q\"";
		var answer = "\"answer\": \"1\"";
		var tolerance = "\"tolerance\": \"0\"";

		// when teacher A adds items with the wrongly typed payloads of the scenario
		var questionsString = addRaw(setup, "{\"questions\": \"Q\"}");
		var questionsObject = addRaw(setup, "{\"questions\": {}}");
		var questionString = addRaw(setup, "{\"questions\": [\"Q\"]}");
		var textNumber = addQuestion(setup, rawQuestion("\"text\": 5", answer, tolerance));
		var textArray = addQuestion(setup, rawQuestion("\"text\": [\"Привет\"]", answer, tolerance));
		var answerDouble = addQuestion(setup, rawQuestion(text, "\"answer\": 1.5", tolerance));
		var answerInteger = addQuestion(setup, rawQuestion(text, "\"answer\": 1", tolerance));
		var answerBoolean = addQuestion(setup, rawQuestion(text, "\"answer\": true", tolerance));
		var answerArray = addQuestion(setup, rawQuestion(text, "\"answer\": [\"1\"]", tolerance));
		var toleranceDouble = addQuestion(setup, rawQuestion(text, answer, "\"tolerance\": 0.5"));
		var toleranceInteger = addQuestion(setup, rawQuestion(text, answer, "\"tolerance\": 0"));
		var toleranceObject = addQuestion(setup, rawQuestion(text, answer, "\"tolerance\": {}"));
		var unitNumber = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": 5"));
		var unitArray = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": [\"км\"]"));

		// then every response is 400 VALIDATION_FAILED with INVALID_TYPE at its own path
		expectValidation(questionsString, error("payload.questions", "INVALID_TYPE"));
		expectValidation(questionsObject, error("payload.questions", "INVALID_TYPE"));
		expectValidation(questionString, error("payload.questions[0]", "INVALID_TYPE"));
		for (Response response : List.of(textNumber, textArray)) {
			expectValidation(response, error("payload.questions[0].text", "INVALID_TYPE"));
		}
		for (Response response : List.of(answerDouble, answerInteger, answerBoolean, answerArray)) {
			expectValidation(response, error("payload.questions[0].answer", "INVALID_TYPE"));
		}
		for (Response response : List.of(toleranceDouble, toleranceInteger, toleranceObject)) {
			expectValidation(response, error("payload.questions[0].tolerance", "INVALID_TYPE"));
		}
		for (Response response : List.of(unitNumber, unitArray)) {
			expectValidation(response, error("payload.questions[0].unit", "INVALID_TYPE"));
		}
		// and no body contains "Привет", and the part has no item
		assertNoEcho(textArray, "Привет");
		assertNoEcho(unitArray, "км");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S11] An unknown property is rejected without echoing its name")
	void anUnknownPropertyIsRejectedWithoutEchoingItsName() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a valid payload that also has an unknown payload property and two unknown question properties
		var inPayload = addRaw(setup, "{\"questions\": [" + rawQuestion(VALID_FIELDS) + "], \"precision\": 2}");
		var points = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"points\": 2"));
		var relative = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"relative\": true"));

		// then all three responses are 400 VALIDATION_FAILED with payload UNKNOWN_FIELD, and no name is echoed
		for (Response response : List.of(inPayload, points, relative)) {
			expectValidation(response, error("payload", "UNKNOWN_FIELD"));
		}
		assertNoEcho(inPayload, "precision");
		assertNoEcho(points, "points");
		assertNoEcho(relative, "relative");
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S12] A NUL in a text or unit and a lone surrogate are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejectedAsPayload() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in a text, a NUL in a unit and a lone high surrogate in a text, written as JSON escapes
		var nulText = addQuestion(setup, rawQuestion("\"text\": \"a\\u0000b\", \"answer\": \"1\", \"tolerance\": \"0\""));
		var nulUnit = addQuestion(setup, rawQuestion(VALID_FIELDS, "\"unit\": \"k\\u0000m\""));
		var surrogate = addQuestion(setup, rawQuestion("\"text\": \"a\\ud800b\", \"answer\": \"1\", \"tolerance\": \"0\""));

		// then all three responses are 400 VALIDATION_FAILED with payload INVALID_FORMAT
		for (Response response : List.of(nulText, nulUnit, surrogate)) {
			expectValidation(response, error("payload", "INVALID_FORMAT"));
		}
		// and no body contains the rejected value, and the part has no item
		assertNoEcho(nulText, "a\u0000b", "a\\u0000b");
		assertNoEcho(nulUnit, "k\u0000m", "k\\u0000m");
		assertNoEcho(surrogate, "a\ud800b", "a\\ud800b");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-numeric/R2-S13] Bean Validation errors come together, a null question and a bad unit come after")
	void beanValidationErrorsComeTogetherAndTheRestComeAfter() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with three Bean Validation errors in one question, a blank unit and a null question,
		// the same with a bad third question, and 26 questions of which the 26th is null
		var threeErrors = add(setup, payload(question("a".repeat(501), "abc", "-1")));
		var blankUnitAndNull = add(setup, payload(questionWithUnit("  "), null));
		var withBadThird = add(setup, payload(
				questionWithUnit("  "),
				null,
				question("Q3", "x", "0")));
		List<Object> questions26 = new ArrayList<>(validQuestions(25));
		questions26.add(null);
		var nullAt26 = add(setup, payload(questions26));

		// then the first has the three errors sorted by field
		expectValidation(
				threeErrors,
				error("payload.questions[0].answer", "INVALID_FORMAT"),
				error("payload.questions[0].text", "TOO_LONG"),
				error("payload.questions[0].tolerance", "INVALID_FORMAT"));
		// and the second has the unit and the null question
		expectValidation(
				blankUnitAndNull,
				error("payload.questions[0].unit", "REQUIRED"),
				error("payload.questions[1]", "REQUIRED"));
		// and the third has only the answer error, the blank unit and the null question not yet reported
		expectValidation(withBadThird, error("payload.questions[2].answer", "INVALID_FORMAT"));
		// and the fourth has only TOO_LONG on the list, the null question not yet reported
		expectValidation(nullAt26, error("payload.questions", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
