package com.teachdash.items.domain.dropdown;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record DropdownPayload(
		@NotEmpty @Size(max = 51) List<@Valid DropdownSegment> segments) {
}
