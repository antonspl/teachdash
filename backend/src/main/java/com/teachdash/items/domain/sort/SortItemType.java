package com.teachdash.items.domain.sort;

import java.util.List;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class SortItemType implements ItemType<SortPayload> {

	@Override
	public String code() {
		return "sort";
	}

	@Override
	public Class<SortPayload> payloadClass() {
		return SortPayload.class;
	}

	@Override
	public Object studentView(SortPayload payload) {
		// STUB: placeholder view, replaced in task 3.3
		return new StudentView(List.of(), List.of());
	}

	@Override
	public List<FieldErrorDetail> extraChecks(SortPayload payload) {
		// STUB: no checks yet, replaced in task 3.2
		return List.of();
	}

	record StudentView(
			List<String> titles,
			List<String> words) {
	}
}
