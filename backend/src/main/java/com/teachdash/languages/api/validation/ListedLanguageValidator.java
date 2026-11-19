package com.teachdash.languages.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ListedLanguageValidator implements ConstraintValidator<ListedLanguage, String> {

	private final ListedLanguages languages;

	ListedLanguageValidator(ListedLanguages languages) {
		this.languages = languages;
	}

	@Override
	public boolean isValid(String code, ConstraintValidatorContext context) {
		return code == null || languages.isListed(code);
	}
}
