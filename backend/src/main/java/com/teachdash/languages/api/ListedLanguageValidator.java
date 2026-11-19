package com.teachdash.languages.api;

import com.teachdash.languages.persistence.LanguageRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ListedLanguageValidator implements ConstraintValidator<ListedLanguage, String> {

	private final LanguageRepository languages;

	ListedLanguageValidator(LanguageRepository languages) {
		this.languages = languages;
	}

	@Override
	public boolean isValid(String code, ConstraintValidatorContext context) {
		return code == null || (code.length() == 2 && languages.exists(code));
	}
}
