package com.teachdash.common;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs before the security filter chain, so a limited request gets 429 before CSRF or session checks.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class JoinLinkRateLimitFilter extends OncePerRequestFilter {

	private static final String PREVIEW_PATH = "/api/join-links/preview";
	private static final Duration SWEEP_INTERVAL = Duration.ofSeconds(1);
	private static final String JOIN_PATH = "/api/join-links/join";

	private final Clock clock;
	private final Map<String, Limiter> limiters;

	JoinLinkRateLimitFilter(
			Clock clock,
			@Value("${teachdash.rate-limit.preview.max}") int previewMax,
			@Value("${teachdash.rate-limit.preview.window}") Duration previewWindow,
			@Value("${teachdash.rate-limit.join.max}") int joinMax,
			@Value("${teachdash.rate-limit.join.window}") Duration joinWindow,
			@Value("${teachdash.rate-limit.max-keys}") int maxKeys) {
		this.clock = clock;
		this.limiters = Map.of(
				PREVIEW_PATH, new Limiter(previewMax, previewWindow, maxKeys),
				JOIN_PATH, new Limiter(joinMax, joinWindow, maxKeys));
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		Limiter limiter = "POST".equals(request.getMethod()) ? limiters.get(request.getRequestURI()) : null;
		if (limiter == null) {
			chain.doFilter(request, response);
			return;
		}
		long retryAfter = limiter.acquire(request.getRemoteAddr(), clock.instant());
		if (retryAfter == 0) {
			chain.doFilter(request, response);
			return;
		}
		response.setHeader("Retry-After", Long.toString(retryAfter));
		ProblemJson.write(response, 429, "Too Many Requests", "RATE_LIMITED");
	}

	private record Window(
			Instant start,
			int count) {
	}

	private static final class Limiter {

		private final int max;
		private final Duration window;
		private final int maxKeys;
		private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
		private final AtomicReference<Instant> lastSweep = new AtomicReference<>(Instant.MIN);

		Limiter(int max, Duration window, int maxKeys) {
			this.max = max;
			this.window = window;
			this.maxKeys = maxKeys;
		}

		/** Returns 0 when the request is allowed, otherwise the Retry-After seconds. */
		long acquire(String key, Instant now) {
			sweepIfFull(now);
			long[] retryAfter = { 0 };
			windows.compute(key, (k, current) -> {
				if (current == null || expired(current, now)) {
					if (current == null && windows.size() >= maxKeys) {
						retryAfter[0] = clampedSeconds(window.getSeconds());
						return null;
					}
					return new Window(now, 1);
				}
				if (current.count() >= max) {
					retryAfter[0] = secondsUntilEnd(current, now);
				}
				return new Window(current.start(), current.count() + 1);
			});
			return retryAfter[0];
		}

		private void sweepIfFull(Instant now) {
			if (windows.size() < maxKeys) {
				return;
			}
			Instant last = lastSweep.get();
			if (now.isBefore(last.plus(SWEEP_INTERVAL)) || !lastSweep.compareAndSet(last, now)) {
				return;
			}
			windows.values().removeIf(w -> expired(w, now));
		}

		private boolean expired(Window w, Instant now) {
			return !now.isBefore(w.start().plus(window));
		}

		private long secondsUntilEnd(Window w, Instant now) {
			Duration left = Duration.between(now, w.start().plus(window));
			long seconds = left.plusNanos(999_999_999).getSeconds();
			return clampedSeconds(seconds);
		}

		private long clampedSeconds(long seconds) {
			return Math.max(1, Math.min(seconds, window.getSeconds()));
		}
	}
}
