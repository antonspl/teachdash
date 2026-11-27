package com.teachdash.items.domain.gapfill;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record GapFillPayload(
		@NotEmpty @Size(max = 51) List<@Valid GapFillSegment> segments) {
}
