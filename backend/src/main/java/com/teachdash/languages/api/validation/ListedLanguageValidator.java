package com.teachdash.languages.api.validation;

import com.teachdash.languages.domain.LanguageCatalog;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ListedLanguageValidator implements ConstraintValidator<ListedLanguage, String> {

	private final LanguageCatalog languages;

	ListedLanguageValidator(LanguageCatalog languages) {
		this.languages = languages;
	}

	@Override
	public boolean isValid(String code, ConstraintValidatorContext context) {
		return code == null || (code.length() == 2 && languages.find(code).isPresent());
	}
}
