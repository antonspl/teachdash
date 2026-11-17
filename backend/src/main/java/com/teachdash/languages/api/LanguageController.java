package com.teachdash.languages.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.languages.api.dto.LanguageResponse;
import com.teachdash.languages.persistence.LanguageRepository;

@RestController
class LanguageController {

	private final LanguageRepository languages;

	LanguageController(LanguageRepository languages) {
		this.languages = languages;
	}

	@GetMapping("/api/languages")
	List<LanguageResponse> languages() {
		return languages.findAll().stream()
				.map(language -> new LanguageResponse(language.code(), language.content()))
				.toList();
	}
}
