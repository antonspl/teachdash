package com.teachdash;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;

class SmokeTest extends ApiTest {

	@Test
	@DisplayName("Application context starts and health is UP")
	void healthIsUp() {
		// given the application is running on a random port

		// when the health endpoint is requested
		var response = given().port(port).when().get("/actuator/health");

		// then it answers 200 with status UP
		response.then().statusCode(200).body("status", equalTo("UP"));
	}
}
