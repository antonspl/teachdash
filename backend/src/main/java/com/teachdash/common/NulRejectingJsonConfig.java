package com.teachdash.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

/** Rejects NUL characters and lone surrogates in every JSON string; Postgres cannot store them (R2-S6). */
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
			if (value != null && (value.indexOf('\0') >= 0 || hasLoneSurrogate(value))) {
				throw new NulCharacterException();
			}
			return value;
		}

		private static boolean hasLoneSurrogate(String value) {
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				boolean validPair = Character.isHighSurrogate(c)
						&& i + 1 < value.length()
						&& Character.isLowSurrogate(value.charAt(i + 1));
				if (validPair) {
					i++;
				}
				else if (Character.isSurrogate(c)) {
					return true;
				}
			}
			return false;
		}
	}

	static class NulCharacterException extends RuntimeException {

		NulCharacterException() {
			super("NUL character in input");
		}
	}
}
