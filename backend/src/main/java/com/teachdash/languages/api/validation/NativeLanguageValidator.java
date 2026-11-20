package com.teachdash.languages.api.validation;

import com.teachdash.languages.domain.LanguageCatalog;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NativeLanguageValidator implements ConstraintValidator<NativeLanguage, String> {

	private final LanguageCatalog languages;

	NativeLanguageValidator(LanguageCatalog languages) {
		this.languages = languages;
	}

	@Override
	public boolean isValid(String code, ConstraintValidatorContext context) {
		if (code == null) {
			return true;
		}
		return code.length() == 2 && languages.find(code).isPresent();
	}
}
