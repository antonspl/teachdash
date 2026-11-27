package com.teachdash.items.domain.truefalse;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class TrueFalseItemType implements ItemType<TrueFalsePayload> {

	@Override
	public String code() {
		return "true-false";
	}

	@Override
	public Class<TrueFalsePayload> payloadClass() {
		return TrueFalsePayload.class;
	}

	@Override
	public Object studentView(TrueFalsePayload payload) {
		// STUB: placeholder, no student view yet
		return Map.of();
	}

	@Override
	public List<FieldErrorDetail> extraChecks(TrueFalsePayload payload) {
		// STUB: no extra checks yet
		return List.of();
	}
}
