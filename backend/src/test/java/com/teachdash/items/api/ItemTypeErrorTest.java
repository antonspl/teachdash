package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** api-errors R1-S11. */
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
}
