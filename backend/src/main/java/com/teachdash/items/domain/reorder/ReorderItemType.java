package com.teachdash.items.domain.reorder;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class ReorderItemType implements ItemType<ReorderPayload> {

	@Override
	public String code() {
		return "reorder";
	}

	@Override
	public Class<ReorderPayload> payloadClass() {
		return ReorderPayload.class;
	}

	// STUB: placeholder view, the real projection comes in pass 2 (task 3.3)
	@Override
	public Object studentView(ReorderPayload payload) {
		return Map.of();
	}

	// STUB: placeholder, the real checks come in pass 2 (task 3.2)
	@Override
	public List<FieldErrorDetail> extraChecks(ReorderPayload payload) {
		return List.of();
	}
}
