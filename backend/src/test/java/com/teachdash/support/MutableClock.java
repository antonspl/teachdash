package com.teachdash.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Clock whose time tests can move (design D6). */
public class MutableClock extends Clock {

	private volatile Instant now = Instant.now();

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return now;
	}

	public void set(Instant instant) {
		this.now = instant;
	}

	public void advance(Duration duration) {
		this.now = now.plus(duration);
	}

	public void reset() {
		this.now = Instant.now();
	}
}
