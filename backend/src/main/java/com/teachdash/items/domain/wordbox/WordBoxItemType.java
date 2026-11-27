package com.teachdash.items.domain.wordbox;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

// STUB: studentView and extraChecks are placeholders until pass 2 (tasks.md 3.2 to 3.6)
@Component
public class WordBoxItemType implements ItemType<WordBoxPayload> {

	@Override
	public String code() {
		return "gap-fill-word-box";
	}

	@Override
	public Class<WordBoxPayload> payloadClass() {
		return WordBoxPayload.class;
	}

	@Override
	public Object studentView(WordBoxPayload payload) {
		return Map.of();
	}

	@Override
	public List<FieldErrorDetail> extraChecks(WordBoxPayload payload) {
		return List.of();
	}
}
