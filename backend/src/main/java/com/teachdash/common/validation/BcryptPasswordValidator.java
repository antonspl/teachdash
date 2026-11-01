package com.teachdash.common.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BcryptPasswordValidator implements ConstraintValidator<BcryptPassword, String> {

	private static final int MIN_CHARACTERS = 8;
	private static final int MAX_BYTES = 72;

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		return password == null
				|| !password.isBlank()
				&& password.length() >= MIN_CHARACTERS
				&& !exceedsBcryptLimit(password);
	}

	/** A password over the limit can never be right, so it never reaches the encoder. */
	public static boolean exceedsBcryptLimit(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
	}
}
