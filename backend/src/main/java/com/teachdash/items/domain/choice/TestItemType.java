package com.teachdash.items.domain.choice;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class TestItemType implements ItemType<TestPayload> {

	@Override
	public String code() {
		return "test";
	}

	@Override
	public Class<TestPayload> payloadClass() {
		return TestPayload.class;
	}

	@Override
	public Object studentView(TestPayload payload) {
		// STUB: pass 2 builds the allowlisted student view
		return Map.of();
	}

	@Override
	public List<FieldErrorDetail> extraChecks(TestPayload payload) {
		// STUB: pass 2 checks null questions and options and repeated option texts
		return List.of();
	}
}
