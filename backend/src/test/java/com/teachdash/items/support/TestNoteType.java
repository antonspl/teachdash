package com.teachdash.items.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.teachdash.common.ApiException;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** Test-only item type: {@code text} is required, other fields such as {@code hint} are kept. */
@Component
public class TestNoteType implements ItemType {

	public static final String CODE = "test-note";
	public static final int SCHEMA_VERSION = 2;
	public static final int MAX_TEXT = 1000;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public int schemaVersion() {
		return SCHEMA_VERSION;
	}

	@Override
	public ObjectNode validate(ObjectNode payload) {
		JsonNode text = payload.get("text");
		if (text == null || text.isNull()) {
			throw error(FieldErrorCodes.REQUIRED);
		}
		if (!text.isString()) {
			throw error(FieldErrorCodes.INVALID_TYPE);
		}
		if (text.stringValue().length() > MAX_TEXT) {
			throw error(FieldErrorCodes.TOO_LONG);
		}
		return payload;
	}

	@Override
	public JsonNode studentProjection(ObjectNode payload) {
		ObjectNode projection = JsonNodeFactory.instance.objectNode();
		projection.set("text", payload.get("text"));
		return projection;
	}

	private static ApiException error(String code) {
		return ApiException.fields(List.of(new FieldErrorDetail("payload.text", code)));
	}
}
