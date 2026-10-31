package com.teachdash.support;

import java.util.Map;
import java.util.UUID;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Cookies via RestAssured's CookieFilter; starts with its own XSRF-TOKEN (double-submit), so no priming GET. */
public class TestClient {

	private final int port;
	private final CookieFilter cookies = new CookieFilter();

	TestClient(int port) {
		this.port = port;
		withCookie("XSRF-TOKEN", UUID.randomUUID().toString());
	}

	private BasicCookieStore store() {
		return (BasicCookieStore) cookies.getCookieStore();
	}

	public TestClient withCookie(String name, String value) {
		BasicClientCookie cookie = new BasicClientCookie(name, value);
		cookie.setDomain("localhost");
		cookie.setPath("/");
		store().addCookie(cookie);
		return this;
	}

	/** The next requests carry neither the XSRF cookie nor the header. */
	public TestClient withoutCsrfToken() {
		var kept = store().getCookies().stream().filter(c -> !c.getName().equals("XSRF-TOKEN")).toList();
		store().clear();
		kept.forEach(store()::addCookie);
		return this;
	}

	public TestClient snapshot() {
		TestClient copy = new TestClient(port);
		copy.store().clear();
		store().getCookies().forEach(copy.store()::addCookie);
		return copy;
	}

	public String cookie(String name) {
		return store().getCookies().stream().filter(c -> c.getName().equals(name)).map(Cookie::getValue)
				.findFirst().orElse(null);
	}

	private RequestSpecification spec() {
		RequestSpecification spec = RestAssured.given().port(port).contentType("application/json").filter(cookies);
		String xsrf = cookie("XSRF-TOKEN");
		return xsrf == null ? spec : spec.header("X-XSRF-TOKEN", xsrf);
	}

	public Response get(String path) {
		return spec().get(path);
	}

	public Response post(String path, Object body) {
		return spec().body(body).post(path);
	}

	public Response post(String path) {
		return spec().post(path);
	}

	public Response patch(String path, Object body) {
		return spec().body(body).patch(path);
	}

	public Response login(String email, String password) {
		return post("/api/auth/login", Map.of("email", email, "password", password));
	}

	public Response logout() {
		return post("/api/auth/logout");
	}
}
