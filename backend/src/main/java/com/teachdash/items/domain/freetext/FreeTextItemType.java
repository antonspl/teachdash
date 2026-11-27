package com.teachdash.items.domain.freetext;

import java.util.List;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
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

	@Override
	public Object studentView(FreeTextPayload payload) {
		return new FreeTextStudentView(payload.prompt(), payload.minLength(), payload.maxLength());
	}

	@Override
	public List<FieldErrorDetail> extraChecks(FreeTextPayload payload) {
		Integer min = payload.minLength();
		Integer max = payload.maxLength();
		if (min != null && max != null && min > max) {
			return List.of(new FieldErrorDetail("maxLength", FieldErrorCodes.INVALID_FORMAT));
		}
		return List.of();
	}
}
