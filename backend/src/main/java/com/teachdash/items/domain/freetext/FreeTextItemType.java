package com.teachdash.items.domain.freetext;

import org.springframework.stereotype.Component;

import com.teachdash.items.domain.ItemType;

@Component
public class FreeTextItemType implements ItemType<FreeTextPayload> {

	@Override
	public String code() {
		return "free-text";
	}

	@Override
	public Class<FreeTextPayload> payloadClass() {
		return FreeTextPayload.class;
	}

	// STUB: returns null until task 3.3; extraChecks stays empty until task 3.2
	@Override
	public Object studentView(FreeTextPayload payload) {
		return null;
	}
}
