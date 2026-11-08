package com.teachdash.enrolment.api.dto;

import java.time.Instant;

import com.teachdash.enrolment.domain.TeacherJoinLinkService.IssuedLink;

public record JoinLinkResponse(
		String link,
		Instant expiresAt) {

	public static JoinLinkResponse from(IssuedLink issued) {
		return new JoinLinkResponse(issued.link(), issued.expiresAt());
	}

	@Override
	public String toString() {
		return "JoinLinkResponse[]";
	}
}
