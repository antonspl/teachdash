package com.teachdash.support;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * One HTTP client with a browser-like cookie jar: cookies are keyed by name and path, a cookie
 * with Max-Age=0 (or a past Expires) is deleted, and Set-Cookie headers are applied in order, so
 * the last one wins. Echoes the XSRF-TOKEN cookie in the X-XSRF-TOKEN header on every request,
 * as the SPA will. A new client starts with a client-made XSRF-TOKEN (double-submit), so even
 * its first POST needs no extra GET; a token set by the server replaces it.
 */
public class TestClient {

	private record Cookie(String name, String path, String value) {
	}

	private record Key(String name, String path) {
	}

	private final int port;
	private final Map<Key, Cookie> jar = new LinkedHashMap<>();

	TestClient(int port) {
		this.port = port;
		put(new Cookie("XSRF-TOKEN", "/", UUID.randomUUID().toString()));
	}

	/** A client that has this cookie (path "/") before its first request, e.g. a session set up by a test. */
	public TestClient withCookie(String name, String value) {
		put(new Cookie(name, "/", value));
		return this;
	}

	/** A new client with a copy of this client's cookies now (e.g. to replay an old session later). */
	public TestClient snapshot() {
		TestClient copy = new TestClient(port);
		copy.jar.clear();
		copy.jar.putAll(jar);
		return copy;
	}

	/** The value of the cookie with this name (any path), or null if the client has none. */
	public String cookie(String name) {
		return jar.values().stream().filter(c -> c.name().equals(name)).map(Cookie::value).findFirst().orElse(null);
	}

	private void put(Cookie c) {
		jar.put(new Key(c.name(), c.path()), c);
	}

	private RequestSpecification spec(String path) {
		RequestSpecification spec = RestAssured.given().port(port).contentType("application/json");
		List<Cookie> matching = new ArrayList<>();
		for (Cookie c : jar.values()) {
			if (pathMatches(path, c.path())) {
				matching.add(c);
			}
		}
		matching.sort(Comparator.comparingInt((Cookie c) -> c.path().length()).reversed());
		if (!matching.isEmpty()) {
			StringBuilder header = new StringBuilder();
			for (Cookie c : matching) {
				header.append(header.isEmpty() ? "" : "; ").append(c.name()).append('=').append(c.value());
			}
			spec.header("Cookie", header.toString());
		}
		matching.stream().filter(c -> c.name().equals("XSRF-TOKEN")).findFirst()
				.ifPresent(c -> spec.header("X-XSRF-TOKEN", c.value()));
		return spec;
	}

	private static boolean pathMatches(String requestPath, String cookiePath) {
		if (requestPath.equals(cookiePath) || cookiePath.equals("/")) {
			return true;
		}
		return requestPath.startsWith(cookiePath) && (cookiePath.endsWith("/")
				|| requestPath.charAt(cookiePath.length()) == '/');
	}

	private Response remember(String requestPath, Response response) {
		for (String header : response.headers().getValues("Set-Cookie")) {
			apply(requestPath, header);
		}
		return response;
	}

	private void apply(String requestPath, String setCookie) {
		String[] parts = setCookie.split(";");
		int eq = parts[0].indexOf('=');
		if (eq < 1) {
			return;
		}
		String name = parts[0].substring(0, eq).trim();
		String value = parts[0].substring(eq + 1).trim();
		String path = null;
		Long maxAge = null;
		ZonedDateTime expires = null;
		for (int i = 1; i < parts.length; i++) {
			String attr = parts[i].trim();
			int e = attr.indexOf('=');
			String key = (e < 0 ? attr : attr.substring(0, e)).toLowerCase(Locale.ROOT);
			String val = e < 0 ? "" : attr.substring(e + 1).trim();
			switch (key) {
				case "path" -> path = val;
				case "max-age" -> {
					try {
						maxAge = Long.parseLong(val);
					} catch (NumberFormatException ignored) {
						// ignored like a browser does
					}
				}
				case "expires" -> {
					try {
						expires = ZonedDateTime.parse(val, DateTimeFormatter.RFC_1123_DATE_TIME);
					} catch (RuntimeException ignored) {
						// ignored like a browser does
					}
				}
				default -> {
				}
			}
		}
		if (path == null || !path.startsWith("/")) {
			int slash = requestPath.lastIndexOf('/');
			path = slash <= 0 ? "/" : requestPath.substring(0, slash);
		}
		boolean expired = maxAge != null ? maxAge <= 0 : expires != null && expires.isBefore(ZonedDateTime.now());
		if (expired) {
			jar.remove(new Key(name, path));
		} else {
			put(new Cookie(name, path, value));
		}
	}

	public Response get(String path) {
		return remember(path, spec(path).get(path));
	}

	public Response post(String path, Object body) {
		return remember(path, spec(path).body(body).post(path));
	}

	public Response post(String path) {
		return remember(path, spec(path).post(path));
	}

	public Response patch(String path, Object body) {
		return remember(path, spec(path).body(body).patch(path));
	}

	/**
	 * Logs in via the JSON login endpoint with one POST. The CSRF token is the XSRF-TOKEN cookie
	 * from an earlier response, or the one this client made itself (double-submit).
	 */
	public Response login(String email, String password) {
		return post("/api/auth/login", Map.of("email", email, "password", password));
	}

	public Response logout() {
		return post("/api/auth/logout");
	}
}
