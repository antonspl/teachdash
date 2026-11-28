package com.teachdash.items.domain.numeric;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class NumericItemType implements ItemType<NumericPayload> {

	@Override
	public String code() {
		return "numeric";
	}

	@Override
	public Class<NumericPayload> payloadClass() {
		return NumericPayload.class;
	}

	@Override
	public Object studentView(NumericPayload payload) {
		if (payload.questions() == null) {
			throw corrupt("question list");
		}
		List<StudentQuestion> questions = payload.questions().stream()
				.map(this::studentQuestion)
				.toList();
		return new StudentView(questions);
	}

	private StudentQuestion studentQuestion(NumericQuestion question) {
		if (question == null || question.text() == null) {
			throw corrupt("question");
		}
		return new StudentQuestion(question.text(), question.unit());
	}

	private IllegalStateException corrupt(String part) {
		return new IllegalStateException("Cannot build a student view for a numeric " + part + " with missing data");
	}

	/** Runs only when Bean Validation passed, so the list is non-empty and at most 25 long, and every non-null question has its text and numbers set; elements may still be null. */
	@Override
	public List<FieldErrorDetail> extraChecks(NumericPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		for (int i = 0; i < payload.questions().size(); i++) {
			NumericQuestion question = payload.questions().get(i);
			String path = "questions[" + i + "]";
			if (question == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
			}
			else if (question.unit() != null) {
				checkUnit(question.unit(), path + ".unit", errors);
			}
		}
		return errors;
	}

	/** Blank is checked first, so a long blank unit is REQUIRED. */
	private void checkUnit(String unit, String path, List<FieldErrorDetail> errors) {
		if (unit.isBlank()) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
		}
		else if (unit.length() > NumericQuestion.MAX_UNIT_LENGTH) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.TOO_LONG));
		}
	}

	record StudentView(
			List<StudentQuestion> questions) {
	}

	@JsonPropertyOrder({"text", "unit"})
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record StudentQuestion(
			String text,
			String unit) {
	}
}
