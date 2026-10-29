package com.teachdash.support;

import java.util.LinkedHashMap;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * One HTTP client with its own cookie jar (session cookie, XSRF-TOKEN). Echoes the
 * XSRF-TOKEN cookie back in the X-XSRF-TOKEN header on every request, as the SPA will.
 */
public class TestClient {

	private final int port;
	private final Map<String, String> cookies = new LinkedHashMap<>();

	TestClient(int port) {
		this.port = port;
	}

	/** Fetches the XSRF-TOKEN cookie so later unsafe requests (POST, PATCH) carry it. */
	public TestClient primeCsrf() {
		get("/api/me");
		return this;
	}

	/** A new client with a copy of this client's cookies now (e.g. to replay an old session later). */
	public TestClient snapshot() {
		TestClient copy = new TestClient(port);
		copy.cookies.putAll(cookies);
		return copy;
	}

	private RequestSpecification spec() {
		RequestSpecification spec = RestAssured.given().port(port).contentType("application/json");
		cookies.forEach(spec::cookie);
		String csrf = cookies.get("XSRF-TOKEN");
		if (csrf != null) {
			spec.header("X-XSRF-TOKEN", csrf);
		}
		return spec;
	}

	private Response remember(Response response) {
		response.getDetailedCookies().asList().forEach(c -> {
			boolean deleted = c.getValue() == null || c.getValue().isEmpty() || c.getMaxAge() == 0;
			if (deleted) {
				cookies.remove(c.getName());
			} else {
				cookies.put(c.getName(), c.getValue());
			}
		});
		return response;
	}

	public Response get(String path) {
		return remember(spec().get(path));
	}

	public Response post(String path, Object body) {
		return remember(spec().body(body).post(path));
	}

	public Response post(String path) {
		return remember(spec().post(path));
	}

	public Response patch(String path, Object body) {
		return remember(spec().body(body).patch(path));
	}

	/** Logs in via the JSON login endpoint and returns the response. */
	public Response login(String email, String password) {
		primeCsrf(); // lets the server hand out the XSRF-TOKEN cookie
		Response response = post("/api/auth/login", Map.of("email", email, "password", password));
		primeCsrf(); // login rotates/clears the XSRF-TOKEN cookie; fetch a fresh one
		return response;
	}

	public Response logout() {
		return post("/api/auth/logout");
	}
}
