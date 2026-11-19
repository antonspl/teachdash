package com.teachdash.languages.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.teachdash.languages.persistence.LanguageRepository;

@Component
public class LanguageCatalog {

	private final List<Language> all;

	private final Map<String, Language> byCode;

	LanguageCatalog(LanguageRepository repository) {
		this.all = List.copyOf(repository.findAll());
		this.byCode = all.stream()
				.collect(Collectors.toUnmodifiableMap(Language::code, Function.identity()));
	}

	public Optional<Language> find(String code) {
		return Optional.ofNullable(byCode.get(code));
	}

	public List<Language> all() {
		return all;
	}
}
