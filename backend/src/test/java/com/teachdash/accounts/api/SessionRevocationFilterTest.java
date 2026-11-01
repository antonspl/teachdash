package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.teachdash.accounts.persistence.UserRepository;

/** R4-S8 needs a failing database, so the filter is called directly with a failing repository. */
class SessionRevocationFilterTest {

	private static final String INTERNAL_DETAIL = "connection to db-host-7 refused";

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("[accounts/R4-S8] A failure in the session check returns a coded error")
	void aFailureInTheSessionCheckReturnsACodedError() throws Exception {
		// given a request with a session
		UserRepository failingUsers = mock(UserRepository.class);
		when(failingUsers.findById(any())).thenThrow(new IllegalStateException(INTERNAL_DETAIL));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		var authorities = AuthorityUtils.createAuthorityList("ROLE_TEACHER");
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						new org.springframework.security.core.userdetails.User(
								UUID.randomUUID().toString(), "", authorities),
						null,
						authorities));

		// when the database fails during the session check
		Exception thrown = null;
		try {
			new SessionRevocationFilter(failingUsers).doFilter(request, response, new MockFilterChain());
		} catch (Exception e) {
			thrown = e;
		}

		// then the response is 500 with code INTERNAL_ERROR and no internal details in the body
		assertNull(thrown);
		assertEquals(500, response.getStatus());
		assertTrue(response.getContentAsString().contains("\"code\":\"INTERNAL_ERROR\""));
		assertFalse(response.getContentAsString().contains(INTERNAL_DETAIL));
	}

	@Test
	@DisplayName("[accounts/R4-S9] The coded error survives a later failure")
	void theCodedErrorSurvivesALaterFailure() throws Exception {
		// given a request with a session
		UserRepository failingUsers = mock(UserRepository.class);
		when(failingUsers.findById(any())).thenThrow(new IllegalStateException(INTERNAL_DETAIL));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		var authorities = AuthorityUtils.createAuthorityList("ROLE_TEACHER");
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						new org.springframework.security.core.userdetails.User(
								UUID.randomUUID().toString(), "", authorities),
						null,
						authorities));

		// when the database fails during the session check
		new SessionRevocationFilter(failingUsers).doFilter(request, response, new MockFilterChain());

		// then the coded error response is already committed when the filter returns
		assertTrue(response.isCommitted());
		assertTrue(response.getContentAsString().contains("\"code\":\"INTERNAL_ERROR\""));
	}
}
