package com.teachdash.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Must run before the session filter, whose failure on commit would otherwise end the response.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class LastResortErrorFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(LastResortErrorFilter.class);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		try {
			chain.doFilter(request, response);
		} catch (RuntimeException e) {
			if (response.isCommitted()) {
				throw e;
			}
			log.error("Request failed outside MVC", e);
			response.reset();
			ProblemJson.write(response, 500, "Internal Server Error", "INTERNAL_ERROR");
		}
	}
}
