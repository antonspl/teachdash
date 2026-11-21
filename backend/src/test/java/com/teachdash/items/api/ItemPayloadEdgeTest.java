package com.teachdash.items.api;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/** Payload values that jsonb cannot store without loss; no scenario ID. */
class ItemPayloadEdgeTest extends ItemApiTest {

	private static String raw(String payloadJson) {
		return "{\"type\":\"test-note\",\"payload\":" + payloadJson + "}";
	}

	private void assertInvalidFormat(Response response, String echoed) {
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("payload", "INVALID_FORMAT"))));
		if (echoed != null) {
			response.then().body(not(containsString(echoed)));
		}
	}

	@Test
	@DisplayName("An unpaired surrogate in a value or a key is rejected and nothing is stored")
	void anUnpairedSurrogateIsRejected() {
		// given
		var setup = teacherWithPart();

		// when
		Response inValue = setup.client().post(setup.items(), raw("{\"text\": \"ab\\ud800cd\"}"));
		Response inKey = setup.client().post(setup.items(), raw("{\"text\": \"ok\", \"k\\udc00\": 1}"));
		Response nested = setup.client().post(setup.items(), raw("{\"text\": \"ok\", \"extra\": [\"x\\ud83d\"]}"));

		// then
		// the parser itself refuses an unpaired surrogate escape; the walk is a second guard
		for (Response response : List.of(inValue, inKey, nested)) {
			response.then().statusCode(400)
					.body("code", anyOf(equalTo("MALFORMED_REQUEST"), equalTo("VALIDATION_FAILED")))
					.body(not(containsString("abcd")));
		}
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("A valid surrogate pair is stored")
	void aValidSurrogatePairIsStored() {
		// given
		var setup = teacherWithPart();

		// when
		Response response = setup.client().post(setup.items(), raw("{\"text\": \"\\ud83d\\ude00\"}"));

		// then
		response.then().statusCode(201);
		assertEquals(1, countItems(setup.part()));
	}

	@Test
	@DisplayName("A number outside the double range is rejected and nothing is stored")
	void aNonFiniteNumberIsRejected() {
		// given
		var setup = teacherWithPart();

		// when
		Response big = setup.client().post(setup.items(), raw("{\"text\": \"ok\", \"n\": 1e400}"));
		Response negative = setup.client().post(setup.items(), raw("{\"text\": \"ok\", \"list\": [-1e999]}"));

		// then
		assertInvalidFormat(big, "1e400");
		assertInvalidFormat(negative, null);
		assertEquals(0, countItems(setup.part()));
	}
}
