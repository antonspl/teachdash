package com.teachdash.items.support;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.teachdash.items.domain.ItemType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Test-only item type: {@code text} is required, {@code hint}, {@code rating} and {@code weight} are optional. */
@Component
public class TestNoteType implements ItemType<TestNoteType.TestNotePayload> {

	public static final String CODE = "test-note";

	public record TestNotePayload(
			@NotNull @Size(max = 1000) String text,
			@Size(max = 200) String hint,
			Integer rating,
			@Digits(integer = 10, fraction = 20) BigDecimal weight) {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public Class<TestNotePayload> payloadClass() {
		return TestNotePayload.class;
	}

	@Override
	public Object studentView(TestNotePayload payload) {
		return Map.of("text", payload.text());
	}
}
