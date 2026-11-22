package com.teachdash.items.domain;

import java.util.List;

import com.teachdash.common.FieldErrorDetail;

public interface ItemType<P> {

	String code();

	Class<P> payloadClass();

	Object studentView(P payload);

	default List<FieldErrorDetail> extraChecks(P payload) {
		return List.of();
	}
}
