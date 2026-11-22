package com.teachdash.common;

import java.math.BigInteger;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;

/** A string, float, boolean or empty string is not an integer: it fails with INVALID_TYPE instead of being coerced. */
@Configuration(proxyBeanMethods = false)
class StrictIntegerJsonConfig {

	@Bean
	JsonMapperBuilderCustomizer strictIntegerCustomizer() {
		return builder -> {
			for (Class<?> type : new Class<?>[] { BigInteger.class, Integer.class, Integer.TYPE, Long.class, Long.TYPE }) {
				builder.withCoercionConfig(type, config -> config
						.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
						.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
						.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
						.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail));
			}
			builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		};
	}
}
