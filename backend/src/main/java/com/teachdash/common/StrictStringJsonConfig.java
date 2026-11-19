package com.teachdash.common;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;

/** A JSON number or boolean is not a String: it fails with INVALID_TYPE instead of being saved as text. */
@Configuration(proxyBeanMethods = false)
class StrictStringJsonConfig {

	@Bean
	JsonMapperBuilderCustomizer strictStringCustomizer() {
		return builder -> builder.withCoercionConfig(String.class, config -> config
				.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
	}
}
