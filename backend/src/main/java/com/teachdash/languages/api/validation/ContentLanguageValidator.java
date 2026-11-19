package com.teachdash.languages.api.validation;

import com.teachdash.languages.domain.Language;
import com.teachdash.languages.persistence.LanguageRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContentLanguageValidator implements ConstraintValidator<ContentLanguage, String> {

	private final LanguageRepository languages;

	ContentLanguageValidator(LanguageRepository languages) {
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
