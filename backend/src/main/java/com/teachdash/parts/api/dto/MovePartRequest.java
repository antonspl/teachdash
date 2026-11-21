package com.teachdash.parts.api.dto;

import java.math.BigInteger;

import jakarta.validation.constraints.NotNull;

public record MovePartRequest(@NotNull BigInteger position) {
}
