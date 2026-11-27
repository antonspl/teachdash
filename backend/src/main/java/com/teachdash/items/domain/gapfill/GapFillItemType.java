package com.teachdash.items.domain.gapfill;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class GapFillItemType implements ItemType<GapFillPayload> {

	@Override
	public String code() {
		return "gap-fill";
	}

	@Override
	public Class<GapFillPayload> payloadClass() {
		return GapFillPayload.class;
	}

	// STUB: placeholder until task 3.3
	@Override
	public Object studentView(GapFillPayload payload) {
		return Map.of();
	}

	// STUB: placeholder until task 3.2
	@Override
	public List<FieldErrorDetail> extraChecks(GapFillPayload payload) {
		return List.of();
	}
}
