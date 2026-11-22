package com.teachdash.items.domain.theory;

import com.teachdash.items.domain.ItemType;

// STUB: not a bean yet, so `theory` stays an unknown type until the implementation pass of items-read-edit
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
		throw new UnsupportedOperationException("STUB");
	}
}
