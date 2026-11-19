package com.teachdash.enrolment.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.languages.api.validation.NativeLanguage;

public record UpdateStudentLanguageRequest(
		@NativeLanguage String nativeLanguage) {

	public UpdateStudentLanguageRequest {
		nativeLanguage = Normalize.strip(nativeLanguage);
	}
}
