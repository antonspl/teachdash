package com.teachdash.languages.api;

import com.teachdash.languages.persistence.LanguageRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NativeLanguageValidator implements ConstraintValidator<NativeLanguage, String> {

	private final LanguageRepository languages;

	NativeLanguageValidator(LanguageRepository languages) {
		this.languages = languages;
	}

	@Override
	public boolean isValid(String code, ConstraintValidatorContext context) {
		return code == null || languages.exists(code);
	}
}
