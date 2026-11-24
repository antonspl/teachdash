package com.teachdash.items.api.dto;

import java.math.BigInteger;

import jakarta.validation.constraints.NotNull;

public record MoveItemRequest(@NotNull BigInteger position) {
}
