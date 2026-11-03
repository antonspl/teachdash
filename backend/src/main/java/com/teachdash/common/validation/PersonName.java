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
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Documented
@NotBlank
@Size(max = 200)
@Pattern(regexp = ".*[^\\p{Z}\\p{C}].*", flags = Pattern.Flag.DOTALL)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface PersonName {

	String message() default "invalid name";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
