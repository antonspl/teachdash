package com.teachdash.common;

import java.util.List;

import org.springframework.stereotype.Component;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;

import jakarta.validation.Validator;

/** An optional title: normalised, blank means none, a present value follows the {@link Title} rules. */
@Component
public class OptionalTitle {

	private static final String FIELD = "title";

	private final Validator validator;

	OptionalTitle(Validator validator) {
		this.validator = validator;
	}

	public String normalise(String title) {
		String normalised = Normalize.name(title);
		return normalised == null || normalised.isBlank() ? null : normalised;
	}

	/** The errors of an already normalised title; empty for null. */
	public List<FieldErrorDetail> errors(String normalised) {
		if (normalised == null) {
			return List.of();
		}
		return FieldErrorCodes.fromViolations(validator.validate(new Holder(normalised)));
	}

	private record Holder(@Title String title) {
	}
}
