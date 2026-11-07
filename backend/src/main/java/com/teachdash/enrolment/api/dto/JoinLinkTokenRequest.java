package com.teachdash.enrolment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinLinkTokenRequest(
		@NotBlank @Size(max = 64) String token) {
}
