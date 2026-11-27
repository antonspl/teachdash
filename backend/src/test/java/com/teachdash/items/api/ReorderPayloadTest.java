package com.teachdash.items.api;

import static com.teachdash.items.support.ReorderPayloads.EMOJI;
import static com.teachdash.items.support.ReorderPayloads.entries;
import static com.teachdash.items.support.ReorderPayloads.entry;
import static com.teachdash.items.support.ReorderPayloads.entryWithHint;
import static com.teachdash.items.support.ReorderPayloads.fullEntries;
import static com.teachdash.items.support.ReorderPayloads.json;
import static com.teachdash.items.support.ReorderPayloads.letters;
import static com.teachdash.items.support.ReorderPayloads.lettersEntry;
import static com.teachdash.items.support.ReorderPayloads.pairEntries;
import static com.teachdash.items.support.ReorderPayloads.payload;
import static com.teachdash.items.support.ReorderPayloads.payloadWithoutMode;
import static com.teachdash.items.support.ReorderPayloads.raw;
import static com.teachdash.items.support.ReorderPayloads.rawWords;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** item-reorder R2. */
class ReorderPayloadTest extends ItemApiTest {

	private static final String VALID_ENTRY = "{\"units\": [\"a\", \"b\"]}";

	private Response add(Setup setup, Object payload) {
		return setup.client().post(setup.items(), body("reorder", payload));
	}

	private Response addRaw(Setup setup, String payloadJson) {
		return setup.client().post(setup.items(), rawBody("reorder", payloadJson));
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
	@DisplayName("[item-reorder/R2-S1] A payload at every limit is accepted in both languages")
	void aPayloadAtEveryLimitIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds the eight payloads (a) to (h) of the scenario
		var twoUnits = payload("words", entry("a", "b"));
		var twentyFiveEntries = payload("words", pairEntries(25));
		var twentyFiveUnits = payload("letters", entryWithHint("Я".repeat(100), letters(25)));
		var cyrillicWord = payload("words", entry("a", "Я".repeat(100)));
		var longSentence = payload("sentences", entry("a", "x".repeat(500)));
		var shortGroups = payload(
				"letters",
				entry("ch", "a", EMOJI.repeat(2)),
				entry("é", "a"));
		var twoHundredUnits = payload("letters", fullEntries(8));
		var nullHint = payload("words", entryWithHint(null, "a", "b"));
		var responses = List.of(
				add(setup, twoUnits),
				add(setup, twentyFiveEntries),
				add(setup, twentyFiveUnits),
				add(setup, cyrillicWord),
				add(setup, longSentence),
				add(setup, shortGroups),
				add(setup, twoHundredUnits),
				addRaw(setup, rawWords("{\"units\": [\"a\", \"b\"], \"hint\": null}")));

		// then all eight responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones
		assertCreated(responses.get(0), twoUnits, 1);
		assertCreated(responses.get(1), twentyFiveEntries, 2);
		assertCreated(responses.get(2), twentyFiveUnits, 3);
		assertCreated(responses.get(3), cyrillicWord, 4);
		assertCreated(responses.get(4), longSentence, 5);
		assertCreated(responses.get(5), shortGroups, 6);
		assertCreated(responses.get(6), twoHundredUnits, 7);
		// and for (h) no hint property is returned or stored
		var withoutHint = payload("words", entry("a", "b"));
		assertCreated(responses.get(7), withoutHint, 8);
		Map<String, Object> returnedEntry = responses.get(7).path("payload.entries[0]");
		assertFalse(returnedEntry.containsKey("hint"));
		assertFalse(readItem(UUID.fromString(responses.get(7).path("id"))).payload().contains("hint"));
	}

	@Test
	@DisplayName("[item-reorder/R2-S2] A unit, a hint or a list over its limit is too long")
	void somethingOverItsLimitIsTooLong() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds the eight payloads (a) to (h) of the scenario
		var entries26 = add(setup, payload("words", pairEntries(26)));
		var units26 = add(setup, payload("letters", lettersEntry(26)));
		var longWord = add(setup, payload("words", entry("a", "x".repeat(101))));
		var longSentence = add(setup, payload("sentences", entry("a", "Я".repeat(501))));
		var longLetter = add(setup, payload("letters", entry("a", "abcde")));
		var longEmoji = add(setup, payload("letters", entry("a", EMOJI.repeat(3))));
		var longHint = add(setup, payload("words", entryWithHint("h".repeat(101), "a", "b")));
		List<Object> over200 = new ArrayList<>(fullEntries(7));
		over200.add(lettersEntry(13));
		over200.add(lettersEntry(13));
		var units201 = add(setup, payload("letters", over200));

		// then the errors are the TOO_LONG errors of the scenario
		expectValidation(entries26, error("payload.entries", "TOO_LONG"));
		expectValidation(units26, error("payload.entries[0].units", "TOO_LONG"));
		expectValidation(longWord, error("payload.entries[0].units[1]", "TOO_LONG"));
		expectValidation(longSentence, error("payload.entries[0].units[1]", "TOO_LONG"));
		expectValidation(longLetter, error("payload.entries[0].units[1]", "TOO_LONG"));
		expectValidation(longEmoji, error("payload.entries[0].units[1]", "TOO_LONG"));
		expectValidation(longHint, error("payload.entries[0].hint", "TOO_LONG"));
		expectValidation(units201, error("payload.entries", "TOO_LONG"));
		// and no body contains a rejectedValue or the sent text
		assertNoEcho(entries26);
		assertNoEcho(units26);
		assertNoEcho(longWord, "xxxxxxxxxx");
		assertNoEcho(longSentence, "ЯЯЯЯЯЯЯЯЯЯ");
		assertNoEcho(longLetter, "abcde");
		assertNoEcho(longEmoji, EMOJI.repeat(3));
		assertNoEcho(longHint, "hhhhhhhhhh");
		assertNoEcho(units201);
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S3] Missing, null or empty entries and units are required, and a single unit is too short")
	void missingNullOrEmptyEntriesAndUnitsAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the seven payloads of the scenario
		var noEntries = addRaw(setup, "{\"mode\": \"words\"}");
		var nullEntries = addRaw(setup, "{\"mode\": \"words\", \"entries\": null}");
		var emptyEntries = addRaw(setup, "{\"mode\": \"words\", \"entries\": []}");
		var noUnits = addRaw(setup, rawWords("{}"));
		var nullUnits = addRaw(setup, rawWords("{\"units\": null}"));
		var emptyUnits = addRaw(setup, rawWords("{\"units\": []}"));
		var oneUnit = addRaw(setup, rawWords("{\"units\": [\"only\"]}"));

		// then the first three are payload.entries REQUIRED, the next three payload.entries[0].units REQUIRED and the last TOO_SHORT
		expectValidation(noEntries, error("payload.entries", "REQUIRED"));
		expectValidation(nullEntries, error("payload.entries", "REQUIRED"));
		expectValidation(emptyEntries, error("payload.entries", "REQUIRED"));
		expectValidation(noUnits, error("payload.entries[0].units", "REQUIRED"));
		expectValidation(nullUnits, error("payload.entries[0].units", "REQUIRED"));
		expectValidation(emptyUnits, error("payload.entries[0].units", "REQUIRED"));
		expectValidation(oneUnit, error("payload.entries[0].units", "TOO_SHORT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S4] A null entry is required at its index")
	void aNullEntryIsRequiredAtItsIndex() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a null second entry, and with two null first entries
		var nullSecond = add(setup, payload("words", entry("a", "b"), null));
		var nullFirstTwo = add(setup, payload("words", null, null, entry("a", "b")));

		// then both responses are 400 with the REQUIRED errors at the null indexes
		expectValidation(nullSecond, error("payload.entries[1]", "REQUIRED"));
		expectValidation(
				nullFirstTwo,
				error("payload.entries[0]", "REQUIRED"),
				error("payload.entries[1]", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S5] A missing, null, blank or unknown mode is rejected")
	void aMissingNullBlankOrUnknownModeIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the entry a, b and the mode missing, null, "", "  ", "Words", "word", "paragraphs", "letters "
		var missing = add(setup, payloadWithoutMode(entry("a", "b")));
		var nullMode = add(setup, payload(null, entry("a", "b")));
		var empty = add(setup, payload("", entry("a", "b")));
		var blank = add(setup, payload("  ", entry("a", "b")));
		var upperCase = add(setup, payload("Words", entry("a", "b")));
		var singular = add(setup, payload("word", entry("a", "b")));
		var paragraphs = add(setup, payload("paragraphs", entry("a", "b")));
		var trailingSpace = add(setup, payload("letters ", entry("a", "b")));

		// then the first four are payload.mode REQUIRED and the last four payload.mode INVALID_FORMAT
		expectValidation(missing, error("payload.mode", "REQUIRED"));
		expectValidation(nullMode, error("payload.mode", "REQUIRED"));
		expectValidation(empty, error("payload.mode", "REQUIRED"));
		expectValidation(blank, error("payload.mode", "REQUIRED"));
		expectValidation(upperCase, error("payload.mode", "INVALID_FORMAT"));
		expectValidation(singular, error("payload.mode", "INVALID_FORMAT"));
		expectValidation(paragraphs, error("payload.mode", "INVALID_FORMAT"));
		expectValidation(trailingSpace, error("payload.mode", "INVALID_FORMAT"));
		// and the unknown mode is not echoed, and the part has no item
		assertNoEcho(paragraphs, "paragraphs");
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S6] A blank or null unit and a blank hint are required at their index")
	void aBlankOrNullUnitAndABlankHintAreRequired() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with the mode words and the payloads (a) to (f) of the scenario
		var emptyUnit = add(setup, payload("words", entry("a", "", "b")));
		var spacesUnit = add(setup, payload("words", entry("a", "  ", "b")));
		var nullUnit = add(setup, payload("words", entry("a", null, "b")));
		var blankInTwoEntries = add(setup, payload("words", entry("a", "\n\t"), entry(" ", "b", "")));
		var emptyHint = add(setup, payload("words", entryWithHint("", "a", "b")));
		var blankHint = add(setup, payload("words", entryWithHint("  ", "a", "b")));

		// then the errors are the REQUIRED errors of the scenario
		expectValidation(emptyUnit, error("payload.entries[0].units[1]", "REQUIRED"));
		expectValidation(spacesUnit, error("payload.entries[0].units[1]", "REQUIRED"));
		expectValidation(nullUnit, error("payload.entries[0].units[1]", "REQUIRED"));
		expectValidation(
				blankInTwoEntries,
				error("payload.entries[0].units[1]", "REQUIRED"),
				error("payload.entries[1].units[0]", "REQUIRED"),
				error("payload.entries[1].units[2]", "REQUIRED"));
		expectValidation(emptyHint, error("payload.entries[0].hint", "REQUIRED"));
		expectValidation(blankHint, error("payload.entries[0].hint", "REQUIRED"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S7] An entry needs at least 2 distinct units, other duplicates are allowed")
	void anEntryNeedsTwoDistinctUnits() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds letters items with a repeated pair in the second entry and with three equal units
		var repeatedPair = add(setup, payload("letters", entry("a", "b"), entry("a", "a")));
		var threeEqual = add(setup, payload("letters", entry("x", "x", "x")));

		// then both responses are 400 with INVALID_FORMAT at the units of the entry, and the part has no item
		expectValidation(repeatedPair, error("payload.entries[1].units", "INVALID_FORMAT"));
		expectValidation(threeEqual, error("payload.entries[0].units", "INVALID_FORMAT"));
		assertEquals(0, countItems(setup.part()));

		// when teacher A adds a letters item with "a", "b", "a" and a words item differing only in case, spaces and normalisation
		var repeatedLetter = payload("letters", entry("a", "b", "a"));
		var differentForms = payload("words", entry("Go", "go", " go", "go ", "é", "é"));
		var accepted = add(setup, repeatedLetter);
		var acceptedForms = add(setup, differentForms);

		// then both are 201 with the payloads exactly as sent
		assertCreated(accepted, repeatedLetter, 1);
		assertCreated(acceptedForms, differentForms, 2);
	}

	@Test
	@DisplayName("[item-reorder/R2-S8] A value of the wrong JSON type or an unknown property is rejected")
	void aWrongJsonTypeOrUnknownPropertyIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with wrongly typed payloads and with unknown properties
		var modeNumber = addRaw(setup, raw("5", VALID_ENTRY));
		var modeArray = addRaw(setup, raw("[\"words\"]", VALID_ENTRY));
		var entriesString = addRaw(setup, "{\"mode\": \"words\", \"entries\": \"text\"}");
		var entriesObject = addRaw(setup, "{\"mode\": \"words\", \"entries\": {}}");
		var entryString = addRaw(setup, rawWords("\"text\""));
		var unitsString = addRaw(setup, rawWords("{\"units\": \"Привет\"}"));
		var unitNumber = addRaw(setup, rawWords("{\"units\": [5, \"b\"]}"));
		var hintNumber = addRaw(setup, rawWords("{\"units\": [\"a\", \"b\"], \"hint\": 7}"));
		Map<String, Object> withCaption = new LinkedHashMap<>(payload("words", entry("a", "b")));
		withCaption.put("caption", "Intro");
		var unknownInPayload = add(setup, withCaption);
		var unknownInEntry = addRaw(setup, rawWords("{\"units\": [\"a\", \"b\"], \"id\": \"e-7731\"}"));

		// then the errors are the INVALID_TYPE and UNKNOWN_FIELD errors of the scenario
		expectValidation(modeNumber, error("payload.mode", "INVALID_TYPE"));
		expectValidation(modeArray, error("payload.mode", "INVALID_TYPE"));
		expectValidation(entriesString, error("payload.entries", "INVALID_TYPE"));
		expectValidation(entriesObject, error("payload.entries", "INVALID_TYPE"));
		expectValidation(entryString, error("payload.entries[0]", "INVALID_TYPE"));
		expectValidation(unitsString, error("payload.entries[0].units", "INVALID_TYPE"));
		expectValidation(unitNumber, error("payload.entries[0].units[0]", "INVALID_TYPE"));
		expectValidation(hintNumber, error("payload.entries[0].hint", "INVALID_TYPE"));
		expectValidation(unknownInPayload, error("payload", "UNKNOWN_FIELD"));
		expectValidation(unknownInEntry, error("payload", "UNKNOWN_FIELD"));
		// and the unknown names and the sent text are not echoed
		assertNoEcho(unitsString, "Привет");
		assertNoEcho(unknownInPayload, "caption", "Intro");
		assertNoEcho(unknownInEntry, "e-7731");
		unknownInEntry.then()
				.body(not(containsString("Unrecognized")))
				.body(not(containsString("\"id\"")));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S9] The list sizes are checked first, the other rules after, and all other errors come together")
	void theListSizesAreCheckedFirst() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds 26 entries with a blank unit and an invalid mode, one unit with an invalid mode, and a payload with four kinds of error
		List<Object> manyWithBlank = pairEntries(26);
		manyWithBlank.set(0, entry("", "b"));
		var tooManyEntries = add(setup, payload("Words", manyWithBlank));
		var oneUnit = add(setup, payload("Words", entry("a")));
		var together = add(
				setup,
				payload(
						"letters",
						entries(
								entryWithHint(" ", "a", "b"),
								entry("c", ""),
								null,
								entry("d", "d"))));

		// then the first is TOO_LONG only and the second TOO_SHORT only
		expectValidation(tooManyEntries, error("payload.entries", "TOO_LONG"));
		expectValidation(oneUnit, error("payload.entries[0].units", "TOO_SHORT"));
		// and the third has all four errors, sorted by field, with no distinct-unit error for the entry with a blank unit
		expectValidation(
				together,
				error("payload.entries[0].hint", "REQUIRED"),
				error("payload.entries[1].units[1]", "REQUIRED"),
				error("payload.entries[2]", "REQUIRED"),
				error("payload.entries[3].units", "INVALID_FORMAT"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S10] With an invalid mode the unit length is not checked, the other rules still are")
	void withAnInvalidModeTheUnitLengthIsNotChecked() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a long unit and the modes "Letters" and "", a blank unit and "Letters", and a long unit and letters
		var longUnitUnknownMode = add(setup, payload("Letters", entry("a", "abcdefgh")));
		var longUnitEmptyMode = add(setup, payload("", entry("a", "abcdefgh")));
		var blankUnitUnknownMode = add(setup, payload("Letters", entry("a", "", "b")));
		var longUnitLetters = add(setup, payload("letters", entry("a", "abcdefgh")));

		// then the errors are the ones of the scenario
		expectValidation(longUnitUnknownMode, error("payload.mode", "INVALID_FORMAT"));
		expectValidation(longUnitEmptyMode, error("payload.mode", "REQUIRED"));
		expectValidation(
				blankUnitUnknownMode,
				error("payload.entries[0].units[1]", "REQUIRED"),
				error("payload.mode", "INVALID_FORMAT"));
		expectValidation(longUnitLetters, error("payload.entries[0].units[1]", "TOO_LONG"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[item-reorder/R2-S11] A NUL and a lone surrogate in a value are rejected as `payload`")
	void aNulAndALoneSurrogateAreRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with a NUL in a unit, a lone high surrogate in a unit and in a hint (JSON escapes)
		var nul = addRaw(setup, rawWords("{\"units\": [\"a\\u0000b\", \"c\"]}"));
		var unitSurrogate = addRaw(setup, rawWords("{\"units\": [\"a\", \"b\\ud800z\"]}"));
		var hintSurrogate = addRaw(setup, rawWords("{\"units\": [\"a\", \"b\"], \"hint\": \"h\\ud800\"}"));

		// then all responses are 400 with payload INVALID_FORMAT
		expectValidation(nul, error("payload", "INVALID_FORMAT"));
		expectValidation(unitSurrogate, error("payload", "INVALID_FORMAT"));
		expectValidation(hintSurrogate, error("payload", "INVALID_FORMAT"));
		// and no body contains the rejected value, in lower or upper case escapes or as a replacement character
		assertNoEcho(nul, "a\\u0000b", "a\u0000b");
		assertNoEcho(unitSurrogate, "b\\ud800z", "\\uD800", "\\ud800", "\\UD800", "�");
		assertNoEcho(hintSurrogate, "h\\ud800", "\\uD800", "\\ud800", "\\UD800", "�");
		assertFalse(unitSurrogate.asString().toLowerCase().contains("ud800"));
		assertFalse(hintSurrogate.asString().toLowerCase().contains("ud800"));
		// and the part has no item
		assertEquals(0, countItems(setup.part()));
	}
}
