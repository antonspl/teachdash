package com.teachdash.items.domain;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ItemPayloadValidator {

	public <P> P validate(ItemType<P> type, Map<String, Object> payload) {
		throw new UnsupportedOperationException("STUB");
	}
}
