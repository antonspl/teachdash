package com.teachdash.languages.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** A listed language that courses can be taught in or from. */
@Documented
@ListedLanguage
@ContentLanguage
@Constraint(validatedBy = {})
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface CourseLanguage {

	String message() default "unsupported language";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
