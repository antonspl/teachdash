package com.teachdash.courses.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.Title;
import com.teachdash.languages.api.validation.CourseLanguage;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
		@Title String title,
		@NotBlank @CourseLanguage String instructionLanguage,
		@CourseLanguage String targetLanguage) {

	public CreateCourseRequest {
		title = Normalize.name(title);
		instructionLanguage = Normalize.strip(instructionLanguage);
		targetLanguage = Normalize.strip(targetLanguage);
	}
}
