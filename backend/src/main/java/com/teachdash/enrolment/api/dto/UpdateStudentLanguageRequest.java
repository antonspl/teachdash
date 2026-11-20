package com.teachdash.enrolment.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.languages.api.validation.NativeLanguage;

import jakarta.validation.constraints.NotBlank;

public record UpdateStudentLanguageRequest(
		@NotBlank @NativeLanguage String nativeLanguage) {

	public UpdateStudentLanguageRequest {
		nativeLanguage = Normalize.strip(nativeLanguage);
	}
}
