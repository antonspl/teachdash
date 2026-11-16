package com.teachdash.languages.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class LanguageController {

	// STUB: replaced in pass 2
	@GetMapping("/api/languages")
	List<Object> languages() {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}
}
