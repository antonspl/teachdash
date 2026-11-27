package com.teachdash.items.domain.matching;

import java.util.List;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class MatchingItemType implements ItemType<MatchingPayload> {

	@Override
	public String code() {
		return "matching";
	}

	@Override
	public Class<MatchingPayload> payloadClass() {
		return MatchingPayload.class;
	}

	@Override
	public Object studentView(MatchingPayload payload) {
		// STUB: placeholder view, replaced in task 3.3
		return new StudentView(List.of(), List.of());
	}

	@Override
	public List<FieldErrorDetail> extraChecks(MatchingPayload payload) {
		// STUB: no checks yet, replaced in task 3.2
		return List.of();
	}

	record StudentView(
			List<String> left,
			List<String> right) {
	}
}
