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
import jakarta.validation.constraints.NotBlank;

/** A language code listed in the languages table. */
@Documented
@NotBlank
@ListedLanguage
@Constraint(validatedBy = {})
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface NativeLanguage {

	String message() default "unknown language";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
