package com.teachdash.languages.api.validation;

import org.springframework.stereotype.Component;

import com.teachdash.languages.domain.LanguageCatalog;

/** The one lookup behind {@link ListedLanguage}, also used where an annotation cannot see the role. */
@Component
public class ListedLanguages {

	private final LanguageCatalog languages;

	ListedLanguages(LanguageCatalog languages) {
		this.languages = languages;
	}

	public boolean isListed(String code) {
		return code.length() == 2 && languages.find(code).isPresent();
	}
}
