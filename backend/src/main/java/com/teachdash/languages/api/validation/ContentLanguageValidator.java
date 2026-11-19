package com.teachdash.languages.api.validation;

import com.teachdash.languages.domain.Language;
import com.teachdash.languages.domain.LanguageCatalog;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContentLanguageValidator implements ConstraintValidator<ContentLanguage, String> {

	private final LanguageCatalog languages;

	ContentLanguageValidator(LanguageCatalog languages) {
		this.languages = languages;
	}

	@Override
	public boolean isValid(String code, ConstraintValidatorContext context) {
		if (code == null) {
			return true;
		}
		return languages.find(code)
				.map(Language::content)
				.orElse(true);
	}
}
