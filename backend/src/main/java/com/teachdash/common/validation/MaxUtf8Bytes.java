package com.teachdash.common.validation;

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

@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ ANNOTATION_TYPE, FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface MaxUtf8Bytes {

	int value();

	String message() default "too many bytes";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
