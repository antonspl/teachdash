package com.teachdash.languages.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** A language that is not listed, or is listed with content true; null passes. */
@Documented
@Constraint(validatedBy = ContentLanguageValidator.class)
@Target({ ANNOTATION_TYPE, FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface ContentLanguage {

	String message() default "language is not available for courses";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
