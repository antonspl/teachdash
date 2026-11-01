package com.teachdash.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** A new password: not blank, at least 8 characters, at most 72 bytes (bcrypt's limit). */
@Documented
@Constraint(validatedBy = BcryptPasswordValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface BcryptPassword {

	String message() default "invalid password";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
