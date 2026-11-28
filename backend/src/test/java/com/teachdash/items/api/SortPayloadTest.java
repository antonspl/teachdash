package com.teachdash.items.api;

import static com.teachdash.items.support.SortPayloads.EMOJI;
import static com.teachdash.items.support.SortPayloads.column;
import static com.teachdash.items.support.SortPayloads.columns;
import static com.teachdash.items.support.SortPayloads.json;
import static com.teachdash.items.support.SortPayloads.numberedColumns;
import static com.teachdash.items.support.SortPayloads.payload;
import static com.teachdash.items.support.SortPayloads.rawColumns;
import static com.teachdash.items.support.SortPayloads.words;
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

/** item-sort R2. */
class SortPayloadTest extends ItemApiTest {

	private static final String VALID_COLUMN = "{\"title\": \"A\", \"words\": [\"a\"]}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("sort", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("sort", payloadJson));
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
	@DisplayName("[item-sort/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items: 2 columns, 10 columns of 5 words, 2 columns of 25 words, 100-letter and 50-emoji values
		var twoColumns = payload(column("A", "a"), column("B", "b"));
		var tenColumns = payload(numberedColumns(10, 5));
		var twentyFiveWords = payload(
				column("A", words(1, 25)),
				column("B", words(26, 50)));
		var longValues = payload(
				column("Я".repeat(100), EMOJI.repeat(50)),
				column("B", "x"));
		var responses = List.of(
				add(setup, twoColumns),
				add(setup, tenColumns),
				add(setup, twentyFiveWords),
				add(setup, longValues));

		// then all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), twoColumns, 1);
		assertCreated(responses.get(1), tenColumns, 2);
		assertCreated(responses.get(2), twentyFiveWords, 3);
		assertCreated(responses.get(3), longValues, 4);
	}

	@Test
	@DisplayName("[item-sort/R2-S2] A title, a word or a count over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();
		var validSecond = column("B", "y");

		// when teacher A adds six items, each over one limit
		var columns11 = add(setup, payload(numberedColumns(11, 1)));
		var longTitle = add(setup, payload(column("a".repeat(101), "x"), validSecond));
		var longCyrillicWord = add(setup, payload(column("A", "Я".repeat(101)), validSecond));
		var longEmojiTitle = add(setup, payload(column(EMOJI.repeat(51), "x"), validSecond));
		var longEmojiWord = add(setup, payload(column("A", EMOJI.repeat(51)), validSecond));
		var words26 = add(setup, payload(column("A", words(1, 26)), validSecond));

		// then the responses are 400 VALIDATION_FAILED with the TOO_LONG error of the scenario
		expectValidation(columns11, error("payload.columns", "TOO_LONG"));
		expectValidation(longTitle, error("payload.columns[0].title", "TOO_LONG"));
		expectValidation(longCyrillicWord, error("payload.columns[0].words[0]", "TOO_LONG"));
		expectValidation(longEmojiTitle, error("payload.columns[0].title", "TOO_LONG"));
		expectValidation(longEmojiWord, error("payload.columns[0].words[0]", "TOO_LONG"));
		expectValidation(words26, error("payload.columns[0].words", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(columns11, "T11", "w11");
		assertNoEcho(longTitle, "aaaaaaaaaa");
		assertNoEcho(longCyrillicWord, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longEmojiTitle, EMOJI.repeat(5));
		assertNoEcho(longEmojiWord, EMOJI.repeat(5));
		assertNoEcho(words26, "w26");
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S3] More than 50 words in all is too long at the columns, together with the other structure errors")
	void moreThan50WordsInAllIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds three items: 51 words in 3 columns, 60 words in 10 columns, and 50 words plus a blank word
		var columns51 = add(setup, payload(
				column("A", words(1, 20)),
				column("B", words(21, 40)),
				column("C", words(41, 51))));
		var columns60 = add(setup, payload(numberedColumns(10, 6)));
		var blankCounts = add(setup, payload(
				column("A", words(1, 25)),
				column("B", words(26, 50)),
				column("C", "")));

		// then the first two are TOO_LONG at the columns only
		expectValidation(columns51, error("payload.columns", "TOO_LONG"));
		expectValidation(columns60, error("payload.columns", "TOO_LONG"));
		// and the third also has the REQUIRED error of the blank word, which counts in the total
		expectValidation(
				blankCounts,
				error("payload.columns", "TOO_LONG"),
				error("payload.columns[2].words[0]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S4] Missing, null or empty columns are required, one column is too short, a null column is required")
	void missingNullOrEmptyColumnsAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the payloads {}, {"columns": null}, {"columns": []}, one column and null columns
		var missing = addRaw(setup, "{}");
		var nullColumns = addRaw(setup, "{\"columns\": null}");
		var empty = addRaw(setup, "{\"columns\": []}");
		var oneColumn = addRaw(setup, rawColumns(VALID_COLUMN));
		var nullSecond = add(setup, payload(column("A", "a"), null));
		var nullFirstTwo = add(setup, payload(null, null, column("A", "a")));

		// then the first three are 400 with payload.columns REQUIRED and the fourth with payload.columns TOO_SHORT
		expectValidation(missing, error("payload.columns", "REQUIRED"));
		expectValidation(nullColumns, error("payload.columns", "REQUIRED"));
		expectValidation(empty, error("payload.columns", "REQUIRED"));
		expectValidation(oneColumn, error("payload.columns", "TOO_SHORT"));
		// and the null columns are REQUIRED at their indexes
		expectValidation(nullSecond, error("payload.columns[1]", "REQUIRED"));
		expectValidation(
				nullFirstTwo,
				error("payload.columns[0]", "REQUIRED"),
				error("payload.columns[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S5] A missing, null, empty or blank title is required")
	void aMissingOrBlankTitleIsRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds five items, each with a bad second column after the valid first column
		var noTitle = addRaw(setup, rawColumns(VALID_COLUMN, "{\"words\": [\"b\"]}"));
		var nullTitle = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": null, \"words\": [\"b\"]}"));
		var emptyTitle = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"\", \"words\": [\"b\"]}"));
		var blankTitle = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"  \", \"words\": [\"b\"]}"));
		var whitespaceTitle = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"\\n\\t\", \"words\": [\"b\"]}"));

		// then each is 400 with payload.columns[1].title REQUIRED
		for (Response response : List.of(noTitle, nullTitle, emptyTitle, blankTitle, whitespaceTitle)) {
			expectValidation(response, error("payload.columns[1].title", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S6] Missing, null or empty words are required, and a blank or null word is required at its index")
	void missingWordsAndBlankWordsAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds three items with missing, null and empty words in the second column
		var noWords = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\"}"));
		var nullWords = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\", \"words\": null}"));
		var emptyWords = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\", \"words\": []}"));
		// and four items with an empty, a blank, a whitespace and a null word
		var emptyWord = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\", \"words\": [\"\"]}"));
		var blankWord = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\", \"words\": [\"  \"]}"));
		var whitespaceWord = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\", \"words\": [\"\\n\\t\"]}"));
		var nullWord = addRaw(setup, rawColumns(VALID_COLUMN, "{\"title\": \"B\", \"words\": [null]}"));

		// then the first three have payload.columns[1].words REQUIRED
		for (Response response : List.of(noWords, nullWords, emptyWords)) {
			expectValidation(response, error("payload.columns[1].words", "REQUIRED"));
		}
		// and the other four have payload.columns[1].words[0] REQUIRED
		for (Response response : List.of(emptyWord, blankWord, whitespaceWord, nullWord)) {
			expectValidation(response, error("payload.columns[1].words[0]", "REQUIRED"));
		}
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S7] A repeated title is rejected on the later one, and a value with its own error is not compared")
	void aRepeatedTitleIsRejectedOnTheLaterOne() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items with a repeated title, three equal titles, two blank titles and two long titles
		var repeated = add(setup, payload(column("A", "a"), column("B", "b"), column("A", "c")));
		var repeatedThrice = add(setup, payload(column("A", "a"), column("A", "b"), column("A", "c")));
		var blankTitles = add(setup, payload(column("", "a"), column("", "b")));
		var longTitles = add(setup, payload(column("a".repeat(101), "a"), column("a".repeat(101), "b")));

		// then the repeats are INVALID_FORMAT on the later titles only
		expectValidation(repeated, error("payload.columns[2].title", "INVALID_FORMAT"));
		expectValidation(
				repeatedThrice,
				error("payload.columns[1].title", "INVALID_FORMAT"),
				error("payload.columns[2].title", "INVALID_FORMAT"));
		// and the blank titles are REQUIRED and the long titles TOO_LONG, with no INVALID_FORMAT
		expectValidation(
				blankTitles,
				error("payload.columns[0].title", "REQUIRED"),
				error("payload.columns[1].title", "REQUIRED"));
		expectValidation(
				longTitles,
				error("payload.columns[0].title", "TOO_LONG"),
				error("payload.columns[1].title", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S8] A word may be in one column only: a repeat is rejected at the later occurrence")
	void aWordMayBeInOneColumnOnly() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds four items with repeated words
		var sameColumn = add(setup, payload(column("A", "go", "walk", "go"), column("B", "ride")));
		var otherColumn = add(setup, payload(column("A", "go", "walk"), column("B", "ride", "go")));
		var threeColumns = add(setup, payload(column("A", "x"), column("B", "x"), column("C", "x")));
		var swapped = add(setup, payload(column("A", "a", "b"), column("B", "b", "a")));

		// then each is INVALID_FORMAT on the later occurrences only, in reading order
		expectValidation(sameColumn, error("payload.columns[0].words[2]", "INVALID_FORMAT"));
		expectValidation(otherColumn, error("payload.columns[1].words[1]", "INVALID_FORMAT"));
		expectValidation(
				threeColumns,
				error("payload.columns[1].words[0]", "INVALID_FORMAT"),
				error("payload.columns[2].words[0]", "INVALID_FORMAT"));
		expectValidation(
				swapped,
				error("payload.columns[1].words[0]", "INVALID_FORMAT"),
				error("payload.columns[1].words[1]", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S9] Only an exact repeat is a duplicate, and a title may equal a word")
	void onlyAnExactRepeatIsADuplicate() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item whose words differ by case, spaces and "ё" against "е", and a title equals a word
		var sent = payload(
				column("Go", "go", "Go", " go", "go ", "ёлка", "елка"),
				column("go", "GO"));
		var response = add(setup, sent);

		// then the response is 201 with both columns exactly as sent
		assertCreated(response, sent, 1);
		// and the part has exactly one item
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S10] A word with its own error is not compared, the other words still are")
	void aWordWithItsOwnErrorIsNotCompared() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with blank words around a repeated word, and one with two equal long words
		var blankWords = add(setup, payload(column("A", "go", ""), column("B", "", "go")));
		var longWords = add(setup, payload(column("A", "a".repeat(101), "a".repeat(101)), column("B", "x")));

		// then the blank words are REQUIRED and not compared, and the repeated "go" is INVALID_FORMAT
		expectValidation(
				blankWords,
				error("payload.columns[0].words[1]", "REQUIRED"),
				error("payload.columns[1].words[0]", "REQUIRED"),
				error("payload.columns[1].words[1]", "INVALID_FORMAT"));
		// and the long words are TOO_LONG, with no INVALID_FORMAT
		expectValidation(
				longWords,
				error("payload.columns[0].words[0]", "TOO_LONG"),
				error("payload.columns[0].words[1]", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S11] A value of the wrong JSON type or an unknown property is rejected")
	void aWrongJsonTypeOrUnknownPropertyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with wrongly typed payloads and with unknown properties
		var columnsString = addRaw(setup, "{\"columns\": \"text\"}");
		var columnsObject = addRaw(setup, "{\"columns\": {}}");
		var columnString = addRaw(setup, "{\"columns\": [\"text\"]}");
		var titleNumber = addRaw(setup, rawColumns("{\"title\": 5, \"words\": [\"a\"]}"));
		var wordsString = addRaw(setup, rawColumns("{\"title\": \"A\", \"words\": \"text\"}"));
		var wordNumber = addRaw(setup, rawColumns("{\"title\": \"A\", \"words\": [5]}"));
		var wordArray = addRaw(setup, rawColumns("{\"title\": \"A\", \"words\": [\"a\", [\"Привет\"]]}"));
		Map<String, Object> withCaption = new LinkedHashMap<>(payload(column("A", "a"), column("B", "b")));
		withCaption.put("caption", "Intro");
		var unknownInPayload = add(setup, withCaption);
		var unknownInColumn = addRaw(setup, rawColumns(
				"{\"title\": \"A\", \"words\": [\"a\"], \"id\": \"c-7731\"}",
				"{\"title\": \"B\", \"words\": [\"b\"]}"));

		// then the errors are the INVALID_TYPE and UNKNOWN_FIELD errors of the scenario
		expectValidation(columnsString, error("payload.columns", "INVALID_TYPE"));
		expectValidation(columnsObject, error("payload.columns", "INVALID_TYPE"));
		expectValidation(columnString, error("payload.columns[0]", "INVALID_TYPE"));
		expectValidation(titleNumber, error("payload.columns[0].title", "INVALID_TYPE"));
		expectValidation(wordsString, error("payload.columns[0].words", "INVALID_TYPE"));
		expectValidation(wordNumber, error("payload.columns[0].words[0]", "INVALID_TYPE"));
		expectValidation(wordArray, error("payload.columns[0].words[1]", "INVALID_TYPE"));
		expectValidation(unknownInPayload, error("payload", "UNKNOWN_FIELD"));
		expectValidation(unknownInColumn, error("payload", "UNKNOWN_FIELD"));
		// and the unknown names and the sent text are not echoed
		assertNoEcho(wordArray, "Привет");
		assertNoEcho(unknownInPayload, "caption", "Intro");
		assertNoEcho(unknownInColumn, "c-7731");
		unknownInColumn.then()
				.body(not(containsString("Unrecognized")))
				.body(not(containsString("\"id\"")));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S12] The count limits are checked first, the structure rules after, and all structure errors come together")
	void theCountLimitsAreCheckedFirst() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds 11 columns with a blank first title, one blank column, a column with empty words
		List<Object> elevenWithBlank = numberedColumns(11, 1);
		elevenWithBlank.set(0, column("", "w1"));
		var tooManyColumns = add(setup, payload(elevenWithBlank));
		var singleBlank = add(setup, payload(column("", "a")));
		var emptyWords = add(setup, payload(column("", "a"), column("B")));
		// and a payload with a repeated word, a blank word, a repeated title and a null column
		var together = add(setup, payload(columns(
				column("A", "go", "go", " "),
				column("A", "x"),
				null)));
		// and 11 columns where the first has 26 words
		List<Object> elevenWith26 = numberedColumns(11, 1);
		elevenWith26.set(0, column("T1", words(1, 26)));
		var bothCounts = add(setup, payload(elevenWith26));

		// then the first is TOO_LONG only, the second TOO_SHORT only, and the third reports only the empty words
		expectValidation(tooManyColumns, error("payload.columns", "TOO_LONG"));
		expectValidation(singleBlank, error("payload.columns", "TOO_SHORT"));
		expectValidation(emptyWords, error("payload.columns[1].words", "REQUIRED"));
		// and the fourth has all four structure errors, sorted by field
		expectValidation(
				together,
				error("payload.columns[0].words[1]", "INVALID_FORMAT"),
				error("payload.columns[0].words[2]", "REQUIRED"),
				error("payload.columns[1].title", "INVALID_FORMAT"),
				error("payload.columns[2]", "REQUIRED"));
		// and the fifth has both count errors
		expectValidation(
				bothCounts,
				error("payload.columns", "TOO_LONG"),
				error("payload.columns[0].words", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-sort/R2-S13] A NUL and a lone surrogate in a value are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in a word and a lone high surrogate in a title (JSON escapes)
		var nul = addRaw(setup, rawColumns(
				"{\"title\": \"A\", \"words\": [\"a\\u0000b\"]}",
				"{\"title\": \"B\", \"words\": [\"c\"]}"));
		var surrogate = addRaw(setup, rawColumns(
				"{\"title\": \"A\\ud800z\", \"words\": [\"a\"]}",
				"{\"title\": \"B\", \"words\": [\"b\"]}"));

		// then both responses are 400 with payload INVALID_FORMAT
		expectValidation(nul, error("payload", "INVALID_FORMAT"));
		expectValidation(surrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value
		assertNoEcho(nul, "a\\u0000b", "a\u0000b");
		assertNoEcho(surrogate, "A\\ud800z", "\\uD800", "\\ud800", "\\UD800", "\uFFFD");
		assertFalse(surrogate.asString().toLowerCase().contains("ud800"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
