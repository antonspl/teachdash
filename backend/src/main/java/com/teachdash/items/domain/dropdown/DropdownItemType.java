package com.teachdash.items.domain.dropdown;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

// STUB: studentView and extraChecks are placeholders, rewritten in tasks 3.2 and 3.3
@Component
public class DropdownItemType implements ItemType<DropdownPayload> {

	@Override
	public String code() {
		return "dropdown";
	}

	@Override
	public Class<DropdownPayload> payloadClass() {
		return DropdownPayload.class;
	}

	@Override
	public Object studentView(DropdownPayload payload) {
		return Map.of();
	}

	@Override
	public List<FieldErrorDetail> extraChecks(DropdownPayload payload) {
		return List.of();
	}
}
