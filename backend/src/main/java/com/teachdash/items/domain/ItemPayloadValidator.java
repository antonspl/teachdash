package com.teachdash.items.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teachdash.common.ApiException;
import com.teachdash.common.FieldErrorDetail;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ItemPayloadValidator {

	private static final String PAYLOAD = "payload";

	private final JsonMapper strictMapper;

	private final JsonMapper storedMapper;

	private final Validator validator;

	ItemPayloadValidator(JsonMapper mapper, Validator validator) {
		this.strictMapper = mapper.rebuild()
				.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
		this.storedMapper = mapper.rebuild()
				.changeDefaultPropertyInclusion(value -> value.withValueInclusion(JsonInclude.Include.NON_NULL))
				.build();
		this.validator = validator;
	}

	public <P> P validate(ItemType<P> type, Map<String, Object> payload) {
		P record;
		try {
			record = strictMapper.convertValue(payload, type.payloadClass());
		}
		catch (DatabindException e) {
			throw ApiException.fromJackson(e, PAYLOAD);
		}
		Set<ConstraintViolation<P>> violations = validator.validate(record);
		if (!violations.isEmpty()) {
			throw ApiException.validation(violations, PAYLOAD);
		}
		List<FieldErrorDetail> extra = type.extraChecks(record);
		if (!extra.isEmpty()) {
			throw ApiException.fields(extra);
		}
		return record;
	}

	/** The JSON stored in the database: no null fields. */
	public String toStoredJson(Object record) {
		return storedMapper.writeValueAsString(record);
	}
}
