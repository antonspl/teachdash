package com.teachdash.items.domain.theory;

import org.springframework.stereotype.Component;

import com.teachdash.items.domain.ItemType;

@Component
public class TheoryItemType implements ItemType<TheoryPayload> {

	@Override
	public String code() {
		return "theory";
	}

	@Override
	public Class<TheoryPayload> payloadClass() {
		return TheoryPayload.class;
	}

	@Override
	public Object studentView(TheoryPayload payload) {
		return payload;
	}
}
