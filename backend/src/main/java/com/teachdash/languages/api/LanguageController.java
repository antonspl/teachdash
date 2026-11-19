package com.teachdash.languages.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.languages.api.dto.LanguageResponse;
import com.teachdash.languages.domain.LanguageCatalog;

@RestController
class LanguageController {

	private final LanguageCatalog languages;

	LanguageController(LanguageCatalog languages) {
		this.languages = languages;
	}

	@GetMapping("/api/languages")
	List<LanguageResponse> languages() {
		return languages.all().stream()
				.map(language -> new LanguageResponse(language.code(), language.content()))
				.toList();
	}
}
