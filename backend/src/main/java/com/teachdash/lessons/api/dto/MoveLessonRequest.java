package com.teachdash.lessons.api.dto;

import java.math.BigInteger;

import jakarta.validation.constraints.NotNull;

public record MoveLessonRequest(@NotNull BigInteger position) {
}
