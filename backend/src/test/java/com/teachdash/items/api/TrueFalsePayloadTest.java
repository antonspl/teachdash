package com.teachdash.items.api;

import static com.teachdash.items.support.TrueFalsePayloads.EMOJI;
import static com.teachdash.items.support.TrueFalsePayloads.json;
import static com.teachdash.items.support.TrueFalsePayloads.numbered;
import static com.teachdash.items.support.TrueFalsePayloads.payload;
import static com.teachdash.items.support.TrueFalsePayloads.payloadWith;
import static com.teachdash.items.support.TrueFalsePayloads.rawStatement;
import static com.teachdash.items.support.TrueFalsePayloads.rawStatements;
import static com.teachdash.items.support.TrueFalsePayloads.statement;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-true-false R2. */
class TrueFalsePayloadTest extends ItemApiTest {

	private static final String NO_BREAK_SPACE = "\u00a0";

	private static final String CAT = "{\"text\": \"A cat is an animal.\", \"answer\": \"true\"}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("true-false", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("true-false", payloadJson));
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

	/** Statements with the texts "01a...a" to "25a...a", each 500 characters long, and the answers cycling. */
	private static List<Object> longStatements() {
		List<String> answers = List.of("true", "false", "not-stated");
		List<Object> statements = new ArrayList<>();
		for (int i = 1; i <= 25; i++) {
			String number = String.format("%02d", i);
			String text = number + "a".repeat(498);
			statements.add(statement(text, answers.get((i - 1) % 3)));
		}
		return statements;
	}

	@Test
	@DisplayName("[item-true-false/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 25 statements of 500 letters, one text of 500 Cyrillic letters and one of 250 emoji
		var statements25 = payloadWith(true, longStatements().toArray());
		var cyrillic = payload(statement("Я".repeat(500), "true"));
		var emoji = payload(statement(EMOJI.repeat(250), "false"));
		var responses = List.of(
				add(setup, statements25),
				add(setup, cyrillic),
				add(setup, emoji));

		// then all three responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), statements25, 1);
		assertCreated(responses.get(1), cyrillic, 2);
		assertCreated(responses.get(2), emoji, 3);
	}

	@Test
	@DisplayName("[item-true-false/R2-S2] More than 25 statements, or a text over 500 characters, is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with 26 statements, a text of 501 Cyrillic letters and a text of 251 emoji
		var statements26 = add(setup, payload(numbered(26).toArray()));
		var longCyrillic = add(setup, payload(statement("Я".repeat(501), "false")));
		var longEmoji = add(setup, payload(statement(EMOJI.repeat(251), "false")));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG errors of the scenario
		expectValidation(statements26, error("payload.statements", "TOO_LONG"));
		expectValidation(longCyrillic, error("payload.statements[0].text", "TOO_LONG"));
		expectValidation(longEmoji, error("payload.statements[0].text", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(statements26, "Statement 1");
		assertNoEcho(longCyrillic, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmoji, EMOJI.repeat(5));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S3] A missing, null or empty statements list and a null statement are required")
	void aMissingStatementsListAndANullStatementAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the payloads {}, {"statements": null}, an empty list and a list with a null statement
		var missing = addRaw(setup, "{}");
		var nullList = addRaw(setup, "{\"statements\": null}");
		var emptyList = addRaw(setup, "{\"allowNotStated\": true, \"statements\": []}");
		var nullStatement = addRaw(setup, rawStatements(CAT, "null"));

		// then (a), (b) and (c) have payload.statements REQUIRED
		for (Response response : List.of(missing, nullList, emptyList)) {
			expectValidation(response, error("payload.statements", "REQUIRED"));
		}
		// and (d) has payload.statements[1] REQUIRED
		expectValidation(nullStatement, error("payload.statements[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S4] A missing, null, empty or blank text is required, a no-break space is not blank")
	void aMissingOrBlankTextIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items whose second statement has no text, a null text, an empty text, a whitespace text and a no-break space
		var missing = addRaw(setup, rawStatements(CAT, "{\"answer\": \"true\"}"));
		var nullText = addRaw(setup, rawStatements(CAT, "{\"text\": null, \"answer\": \"true\"}"));
		var empty = addRaw(setup, rawStatements(CAT, rawStatement("", "\"true\"")));
		var whitespace = addRaw(setup, rawStatements(CAT, rawStatement("  \n\t", "\"true\"")));
		var noBreakSpace = add(setup, payload(statement("A cat is an animal.", "true"), statement(NO_BREAK_SPACE, "true")));

		// then the first four are 400 VALIDATION_FAILED with payload.statements[1].text REQUIRED
		for (Response response : List.of(missing, nullText, empty, whitespace)) {
			expectValidation(response, error("payload.statements[1].text", "REQUIRED"));
		}
		// and the no-break space is 201 with the text exactly as sent
		noBreakSpace.then().statusCode(201).body("payload.statements[1].text", equalTo(NO_BREAK_SPACE));
		assertStoredPayload(
				UUID.fromString(noBreakSpace.path("id")),
				json(payload(statement("A cat is an animal.", "true"), statement(NO_BREAK_SPACE, "true"))));
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S5] A repeated text is rejected only on an exact match")
	void aRepeatedTextIsRejectedOnlyOnAnExactMatch() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a repeat of different answers, a triple repeat and four texts that differ by case and spaces
		var repeat = add(setup, payload(
				statement("A", "true"),
				statement("B", "false"),
				statement("A", "false")));
		var triple = add(setup, payload(
				statement("A", "true"),
				statement("A", "true"),
				statement("A", "true")));
		var near = payload(
				statement("Go home.", "true"),
				statement("go home.", "true"),
				statement("Go home. ", "true"),
				statement(" Go home.", "true"));
		var nearResponse = add(setup, near);

		// then the repeat is 400 VALIDATION_FAILED at the later statement
		expectValidation(repeat, error("payload.statements[2].text", "INVALID_FORMAT"));
		// and the triple repeat is 400 at the second and the third
		expectValidation(
				triple,
				error("payload.statements[1].text", "INVALID_FORMAT"),
				error("payload.statements[2].text", "INVALID_FORMAT"));
		// and the near-repeats are 201 with all four texts exactly as sent
		assertCreated(nearResponse, near, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S6] A missing or blank answer is required, any other value than the three is a wrong format")
	void aMissingOrBlankAnswerIsRequiredAndOtherValuesAreWrong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with allowNotStated true whose second statement has a missing, null, empty and blank answer
		List<Response> required = List.of(
				addRaw(setup, withAllow(CAT, "{\"text\": \"A dog is a bird.\"}")),
				addRaw(setup, withAllow(CAT, rawStatement("A dog is a bird.", "null"))),
				addRaw(setup, withAllow(CAT, rawStatement("A dog is a bird.", "\"\""))),
				addRaw(setup, withAllow(CAT, rawStatement("A dog is a bird.", "\"  \""))));
		// and with the answers "True", "TRUE", "yes", " true", "true ", "Not-stated" and "not stated"
		List<String> wrong = List.of("True", "TRUE", "yes", " true", "true ", "Not-stated", "not stated");
		List<Response> wrongFormat = wrong.stream()
				.map(answer -> addRaw(setup, withAllow(CAT, rawStatement("A dog is a bird.", json(answer)))))
				.toList();

		// then all responses are 400 VALIDATION_FAILED, REQUIRED for the first four
		for (Response response : required) {
			expectValidation(response, error("payload.statements[1].answer", "REQUIRED"));
		}
		// and INVALID_FORMAT for the other seven
		for (Response response : wrongFormat) {
			expectValidation(response, error("payload.statements[1].answer", "INVALID_FORMAT"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	private static String withAllow(String... statements) {
		return "{\"allowNotStated\": true, \"statements\": [" + String.join(", ", statements) + "]}";
	}

	@Test
	@DisplayName("[item-true-false/R2-S7] \"not-stated\" needs allowNotStated true")
	void notStatedNeedsAllowNotStated() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a "not-stated" answer and no allowNotStated, a null one and a false one
		var statements = new Object[] {
				statement("A cat is an animal.", "true"),
				statement("The cat is black.", "not-stated")};
		var missing = add(setup, payload(statements));
		var nullAllow = add(setup, payloadWith(null, statements));
		var falseAllow = add(setup, payloadWith(false, statements));
		// and with allowNotStated false and two "not-stated" answers
		var twice = add(setup, payloadWith(
				false,
				statement("A", "not-stated"),
				statement("B", "true"),
				statement("C", "not-stated")));
		// and with allowNotStated true and a single "not-stated" answer
		var accepted = payloadWith(true, statement("The cat is black.", "not-stated"));
		var acceptedResponse = add(setup, accepted);

		// then the first three are 400 VALIDATION_FAILED with payload.statements[1].answer INVALID_FORMAT
		for (Response response : List.of(missing, nullAllow, falseAllow)) {
			expectValidation(response, error("payload.statements[1].answer", "INVALID_FORMAT"));
		}
		// and the fourth is 400 at the first and the third answer
		expectValidation(
				twice,
				error("payload.statements[0].answer", "INVALID_FORMAT"),
				error("payload.statements[2].answer", "INVALID_FORMAT"));
		// and the last is 201 with the payload exactly as sent
		assertCreated(acceptedResponse, accepted, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S8] A value of the wrong JSON type is rejected with its path")
	void aValueOfTheWrongJsonTypeIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a wrong JSON type in allowNotStated, statements, a statement, text and answer
		String cat = "[" + CAT + "]";
		var allowString = addRaw(setup, "{\"allowNotStated\": \"yes\", \"statements\": " + cat + "}");
		var allowArray = addRaw(setup, "{\"allowNotStated\": [true], \"statements\": " + cat + "}");
		var statementsString = addRaw(setup, "{\"statements\": \"text\"}");
		var statementsObject = addRaw(setup, "{\"statements\": {}}");
		var statementString = addRaw(setup, "{\"statements\": [\"text\"]}");
		var textNumber = addRaw(setup, "{\"statements\": [{\"text\": 5, \"answer\": \"true\"}]}");
		var textArray = addRaw(setup, "{\"statements\": [{\"text\": [\"Привет\"], \"answer\": \"true\"}]}");
		var answerBoolean = addRaw(setup, "{\"statements\": [{\"text\": \"A cat is an animal.\", \"answer\": true}]}");
		var answerNumber = addRaw(setup, "{\"statements\": [{\"text\": \"A cat is an animal.\", \"answer\": 1}]}");
		var answerArray = addRaw(setup, "{\"statements\": [{\"text\": \"A cat is an animal.\", \"answer\": [\"true\"]}]}");

		// then every response is 400 VALIDATION_FAILED with INVALID_TYPE at its own path
		expectValidation(allowString, error("payload.allowNotStated", "INVALID_TYPE"));
		expectValidation(allowArray, error("payload.allowNotStated", "INVALID_TYPE"));
		expectValidation(statementsString, error("payload.statements", "INVALID_TYPE"));
		expectValidation(statementsObject, error("payload.statements", "INVALID_TYPE"));
		expectValidation(statementString, error("payload.statements[0]", "INVALID_TYPE"));
		expectValidation(textNumber, error("payload.statements[0].text", "INVALID_TYPE"));
		expectValidation(textArray, error("payload.statements[0].text", "INVALID_TYPE"));
		expectValidation(answerBoolean, error("payload.statements[0].answer", "INVALID_TYPE"));
		expectValidation(answerNumber, error("payload.statements[0].answer", "INVALID_TYPE"));
		expectValidation(answerArray, error("payload.statements[0].answer", "INVALID_TYPE"));
		// and no body contains "Привет", and the part has no item
		assertNoEcho(textArray, "Привет");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S9] An unknown property is rejected without echoing its name")
	void anUnknownPropertyIsRejectedWithoutEchoingItsName() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with an unknown property in the payload and in a statement
		var inPayload = addRaw(setup, "{\"statements\": [" + CAT + "], \"caption\": \"Intro\"}");
		var explanation = addRaw(setup, "{\"statements\": [{\"text\": \"A cat is an animal.\", "
				+ "\"answer\": \"true\", \"explanation\": \"Cats are mammals\"}]}");
		var withId = addRaw(setup, "{\"statements\": [{\"text\": \"A cat is an animal.\", "
				+ "\"answer\": \"true\", \"id\": \"st-7731\"}]}");

		// then all responses are 400 VALIDATION_FAILED with payload UNKNOWN_FIELD
		for (Response response : List.of(inPayload, explanation, withId)) {
			expectValidation(response, error("payload", "UNKNOWN_FIELD"));
		}
		// and no body contains the property names or values
		assertNoEcho(inPayload, "caption", "Intro");
		assertNoEcho(explanation, "explanation", "Cats are mammals");
		assertNoEcho(withId, "st-7731");
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-true-false/R2-S10] Limits and required fields are checked first, the format rules after, and all format errors come together")
	void limitsAreCheckedFirstAndFormatErrorsComeTogether() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds a blank text with a wrong answer, wrong answers with a repeat and a null statement, and 26 statements with a blank text and a wrong answer
		var blankAndWrong = add(setup, payloadWith(
				false,
				statement("A", "maybe"),
				statement("  ", "true")));
		var formatErrors = add(setup, payloadWith(
				false,
				statement("A", "maybe"),
				statement("A", "not-stated"),
				null));
		List<Object> many = numbered(26);
		many.set(0, statement("", "true"));
		many.set(25, statement("Statement 26", "maybe"));
		var tooMany = add(setup, payload(many.toArray()));

		// then the first is 400 VALIDATION_FAILED with only the REQUIRED text error
		expectValidation(blankAndWrong, error("payload.statements[1].text", "REQUIRED"));
		// and the second has all format errors together, sorted by field
		expectValidation(
				formatErrors,
				error("payload.statements[0].answer", "INVALID_FORMAT"),
				error("payload.statements[1].answer", "INVALID_FORMAT"),
				error("payload.statements[1].text", "INVALID_FORMAT"),
				error("payload.statements[2]", "REQUIRED"));
		// and the third has only TOO_LONG and the REQUIRED text
		expectValidation(
				tooMany,
				error("payload.statements", "TOO_LONG"),
				error("payload.statements[0].text", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
