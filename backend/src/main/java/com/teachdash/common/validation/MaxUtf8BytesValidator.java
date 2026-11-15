package com.teachdash.common.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, String> {

	private int max;

	@Override
	public void initialize(MaxUtf8Bytes constraint) {
		this.max = constraint.value();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || !exceeds(value, max);
	}

	public static boolean exceeds(String value, int maxBytes) {
		return value.getBytes(StandardCharsets.UTF_8).length > maxBytes;
	}
}
