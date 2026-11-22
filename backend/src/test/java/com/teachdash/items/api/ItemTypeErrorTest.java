package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** api-errors R1-S11 and R1-S12. */
class ItemTypeErrorTest extends ItemApiTest {

	@Test
	@DisplayName("[api-errors/R1-S11] An unregistered item type is rejected with its own code")
	void anUnregisteredItemTypeIsRejectedWithItsOwnCode() {
		// given a logged-in teacher with a part, and no item type poll registered
		var setup = teacherWithPart();

		// when the teacher adds an item with the type "poll" and the payload {"text": "Hi"}
		var response = setup.client().post(setup.items(), body("poll", Map.of("text", "Hi")));

		// then the response is 400 VALIDATION_FAILED with type UNKNOWN_TYPE
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("type", "UNKNOWN_TYPE"))));
		// and the body does not contain "poll" as a value
		response.then().body(not(containsString("\"poll\"")));
		// and no item is created
		assertEquals(0, countItems(setup.part()));
	}

	@Test
	@DisplayName("[api-errors/R1-S12] An unknown payload property is rejected with its own code")
	void anUnknownPayloadPropertyIsRejectedWithItsOwnCode() {
		// given a logged-in teacher with a part, and the item type test-note
		var setup = teacherWithPart();

		// when the teacher adds an item with the type test-note and the payload {"text": "Hi", "answer": 42}
		var response = setup.client().post(setup.items(), body("test-note", Map.of("text", "Hi", "answer", 42)));

		// then the response is 400 VALIDATION_FAILED with payload UNKNOWN_FIELD only
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("payload", "UNKNOWN_FIELD"))));
		// and the body does not contain "answer"
		response.then().body(not(containsString("answer")));
		// and no item is created
		assertEquals(0, countItems(setup.part()));
	}
}
