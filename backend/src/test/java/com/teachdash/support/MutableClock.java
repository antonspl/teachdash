package com.teachdash.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Movable clock; clocks from withZone share the instant but keep their own zone. */
public class MutableClock extends Clock {

	private final AtomicReference<Instant> now;
	private final ZoneId zone;

	public MutableClock() {
		this(new AtomicReference<>(Instant.now()), ZoneOffset.UTC);
	}

	private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
		this.now = now;
		this.zone = zone;
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableClock(now, zone);
	}

	@Override
	public Instant instant() {
		return now.get();
	}

	public void advance(Duration duration) {
		now.updateAndGet(i -> i.plus(duration));
	}

	public void reset() {
		now.set(Instant.now());
	}
}
