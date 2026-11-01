package com.teachdash.common;

import java.io.IOException;

import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/** Writes the error body for code that runs outside Spring MVC (security filters). */
public final class ProblemJson {

	private ProblemJson() {
	}

	public static void write(
			HttpServletResponse response,
			int status,
			String title,
			String code) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		String body = "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"code\":\"%s\"}"
				.formatted(title, status, code);
		response.getWriter().write(body);
		response.flushBuffer();
	}
}
