package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R11. Own context with low limits; every test uses its own client addresses. */
@TestPropertySource(properties = {
		"teachdash.rate-limit.join.max=2",
		"teachdash.rate-limit.join.window=10m",
		"teachdash.rate-limit.preview.max=2",
		"teachdash.rate-limit.preview.window=10m" })
class JoinLinkRateLimitTest extends ApiTest {

	private static final String PREVIEW = "/api/join-links/preview";
	private static final String JOIN = "/api/join-links/join";

	private TestClient clientFrom(String ip) {
		return newClient().withHeader("X-Forwarded-For", ip);
	}

	private Map<String, Object> joinBody(String token, String email) {
		return Map.of(
				"token", token,
				"name", "Nina",
				"email", email,
				"timeZone", "UTC",
				"password", "new-password-1");
	}

	@Test
	@DisplayName("[student-enrolment/R11-S1] Request over the limit is rejected")
	void requestOverTheLimitIsRejected() {
		// given a join limit of 2 requests per 10 minutes, and teacher A with a join link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient client = clientFrom("203.0.113.11");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when a client from one IP joins through the link with an unknown token twice, and then with a new email
		var first = client.post(JOIN, joinBody("unknown-token-value", "nina@example.com"));
		var second = client.post(JOIN, joinBody("unknown-token-value", "nina@example.com"));
		var third = client.post(JOIN, joinBody(token, "nina@example.com"));

		// then the first two responses are 410
		first.then().statusCode(410);
		second.then().statusCode(410);
		// and the third is 429 with code RATE_LIMITED and a Retry-After header between 1 and 600
		third.then().statusCode(429).body("code", equalTo("RATE_LIMITED"));
		long retryAfter = Long.parseLong(third.header("Retry-After"));
		org.hamcrest.MatcherAssert.assertThat(retryAfter, greaterThanOrEqualTo(1L));
		org.hamcrest.MatcherAssert.assertThat(retryAfter, lessThanOrEqualTo(600L));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R11-S2] Limit is per IP and per endpoint")
	void limitIsPerIpAndPerEndpoint() {
		// given a join limit of 2 and a preview limit of 2 requests per 10 minutes, teacher A with a join link, and a client from IP 1 that has used up the join limit
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient ipOne = clientFrom("203.0.113.12");
		ipOne.post(JOIN, joinBody("unknown-token-value", "x@example.com"));
		ipOne.post(JOIN, joinBody("unknown-token-value", "x@example.com"));

		// when a client from IP 2 joins through the link with a new email, and the client from IP 1 requests the preview and logs in as an existing teacher
		var joinFromIpTwo = clientFrom("198.51.100.12").post(JOIN, joinBody(token, "nina@example.com"));
		var previewFromIpOne = ipOne.post(PREVIEW, Map.of("token", token));
		var loginFromIpOne = ipOne.login("a@example.com", "teacher-pass-1");

		// then the join from IP 2 is 201, the preview from IP 1 is 200, and the login from IP 1 is 200
		joinFromIpTwo.then().statusCode(201);
		previewFromIpOne.then().statusCode(200);
		loginFromIpOne.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R11-S3] Limit resets after the window")
	void limitResetsAfterTheWindow() {
		// given a preview limit of 2 requests per 10 minutes, teacher A with a join link, and a client that has used up the preview limit
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient client = clientFrom("203.0.113.13");
		client.post(PREVIEW, Map.of("token", token));
		client.post(PREVIEW, Map.of("token", token));

		// when 10 minutes pass and the client requests the preview again
		testClock().advance(Duration.ofMinutes(10));
		var response = client.post(PREVIEW, Map.of("token", token));

		// then the response is 200
		response.then().statusCode(200);
	}
}
