package com.teachdash.accounts;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;

class SmokeTest extends ApiTest {

	@Test
	void contextStarts() {
		assertNotNull(jdbc);
		assertNotNull(newClient());
	}
}
