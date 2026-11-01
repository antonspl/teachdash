package com.teachdash.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

/** Rejects NUL characters in every JSON string; Postgres cannot store them (R2-S6). */
@Configuration(proxyBeanMethods = false)
class NulRejectingJsonConfig {

	@Bean
	JacksonModule nulRejectingModule() {
		SimpleModule module = new SimpleModule("nulRejecting");
		module.addDeserializer(String.class, new NulRejectingStringDeserializer());
		return module;
	}

	static class NulRejectingStringDeserializer extends StringDeserializer {

		@Override
		public String deserialize(JsonParser parser, DeserializationContext context) {
			String value = super.deserialize(parser, context);
			if (value != null && value.indexOf('\0') >= 0) {
				throw new NulCharacterException();
			}
			return value;
		}
	}

	static class NulCharacterException extends RuntimeException {

		NulCharacterException() {
			super("NUL character in input");
		}
	}
}
