package com.teachdash.common;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;

/** A string, number or empty string is not a boolean: it fails with INVALID_TYPE instead of being coerced. */
@Configuration(proxyBeanMethods = false)
class StrictBooleanJsonConfig {

	@Bean
	JsonMapperBuilderCustomizer strictBooleanCustomizer() {
		return builder -> {
			for (Class<?> type : new Class<?>[] { Boolean.class, Boolean.TYPE }) {
				builder.withCoercionConfig(type, config -> config
						.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
						.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
						.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
						.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail));
			}
		};
	}
}
