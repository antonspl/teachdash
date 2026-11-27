package com.teachdash.items.domain.numeric;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class NumericItemType implements ItemType<NumericPayload> {

	@Override
	public String code() {
		return "numeric";
	}

	@Override
	public Class<NumericPayload> payloadClass() {
		return NumericPayload.class;
	}

	// STUB: placeholder student view (tasks 3.3)
	@Override
	public Object studentView(NumericPayload payload) {
		return Map.of();
	}

	// STUB: placeholder, no extra checks yet (tasks 3.2)
	@Override
	public List<FieldErrorDetail> extraChecks(NumericPayload payload) {
		return List.of();
	}
}
