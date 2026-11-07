package com.teachdash.enrolment.api.dto;

public record JoinLinkResponse(
		String link) {

	@Override
	public String toString() {
		return "JoinLinkResponse[]";
	}
}
