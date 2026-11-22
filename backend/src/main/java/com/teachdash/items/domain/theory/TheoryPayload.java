package com.teachdash.items.domain.theory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TheoryPayload(
		@NotBlank @Size(max = 20000) String text) {
}
