package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockingDetails;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.teachdash.support.ApiTest;

/** R10 login timing. Counts hash checks instead of measuring time, which would be flaky. */
class LoginTimingTest extends ApiTest {

	private static final Pattern BCRYPT_COST = Pattern.compile("\\$2[abxy]\\$(\\d{2})\\$");

	@MockitoSpyBean
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("[accounts/R10-S1] A login with no real hash to check still runs one hash check")
	void aLoginWithNoRealHashToCheckStillRunsOneHashCheck() {
		// given an active teacher with a password, and active and deactivated teachers with no password
		createTeacher("with-password@example.com", "teacher-pass-1");
		createTeacher("no-password@example.com", null);
		createUser("TEACHER", "Teacher", "deactivated@example.com", null, false);
		// and the cost of a hash made for a new password
		int newPasswordCost = costOf(passwordEncoder.encode("reference-password-1"));

		// when someone logs in with each of the cases below
		// then each login is 401 and runs exactly one hash check at the cost of a new password
		assertOneHashCheck("unknown email", "nobody@example.com", "whatever-123", newPasswordCost);
		assertOneHashCheck("active teacher with no password",
				"no-password@example.com", "any-password-1", newPasswordCost);
		assertOneHashCheck("deactivated teacher with no password",
				"deactivated@example.com", "any-password-1", newPasswordCost);
		assertOneHashCheck("73-character password",
				"with-password@example.com", "a".repeat(73), newPasswordCost);
	}

	private void assertOneHashCheck(String label, String email, String password, int expectedCost) {
		clearInvocations(passwordEncoder);

		var login = newClient().login(email, password);

		assertEquals(401, login.statusCode(), label + ": status");
		List<Invocation> checks = mockingDetails(passwordEncoder).getInvocations().stream()
				.filter(invocation -> invocation.getMethod().getName().equals("matches"))
				.toList();
		assertEquals(1, checks.size(), label + ": number of hash checks");
		String hash = checks.get(0).getArgument(1, String.class);
		assertNotNull(hash, label + ": hash checked");
		assertEquals(expectedCost, costOf(hash), label + ": cost of the checked hash");
	}

	private static int costOf(String hash) {
		Matcher matcher = BCRYPT_COST.matcher(hash);
		if (!matcher.find()) {
			throw new IllegalStateException("not a bcrypt hash: " + hash);
		}
		return Integer.parseInt(matcher.group(1));
	}
}
