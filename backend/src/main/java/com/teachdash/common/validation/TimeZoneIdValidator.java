package com.teachdash.common.validation;

import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimeZoneIdValidator implements ConstraintValidator<TimeZoneId, String> {

	@Override
	public boolean isValid(String timeZone, ConstraintValidatorContext context) {
		return timeZone == null || ZoneId.getAvailableZoneIds().contains(timeZone);
	}
}
