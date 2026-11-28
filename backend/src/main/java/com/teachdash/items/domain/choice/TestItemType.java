package com.teachdash.items.domain.choice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class TestItemType implements ItemType<TestPayload> {

	@Override
	public String code() {
		return "test";
	}

	@Override
	public Class<TestPayload> payloadClass() {
		return TestPayload.class;
	}

	@Override
	public Object studentView(TestPayload payload) {
		if (payload == null || payload.questions() == null) {
			throw corrupt("question list");
		}
		List<StudentQuestion> questions = payload.questions().stream()
				.map(this::studentQuestion)
				.toList();
		return new StudentView(questions);
	}

	private StudentQuestion studentQuestion(TestQuestion question) {
		if (question == null || question.text() == null || question.options() == null) {
			throw corrupt("question");
		}
		List<StudentOption> options = new ArrayList<>();
		for (TestOption option : question.options()) {
			requireComplete(option);
			options.add(new StudentOption(option.text()));
		}
		return new StudentQuestion(question.text(), Boolean.TRUE.equals(question.multiple()), options);
	}

	private void requireComplete(TestOption option) {
		if (option == null || option.text() == null || option.correct() == null) {
			throw corrupt("option");
		}
	}

	private IllegalStateException corrupt(String part) {
		return new IllegalStateException("Cannot build a student view for a test " + part + " with missing data");
	}

	record StudentView(
			List<StudentQuestion> questions) {
	}

	@JsonPropertyOrder({"text", "multiple", "options"})
	record StudentQuestion(
			String text,
			boolean multiple,
			List<StudentOption> options) {
	}

	record StudentOption(
			String text) {
	}

	/** Runs only when Bean Validation passed, so the lists are non-empty and the texts are set. */
	@Override
	public List<FieldErrorDetail> extraChecks(TestPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		for (int i = 0; i < payload.questions().size(); i++) {
			TestQuestion question = payload.questions().get(i);
			String path = "questions[" + i + "]";
			if (question == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				continue;
			}
			checkOptions(question.options(), path + ".options", errors);
			checkSeveralCorrect(question, path + ".options", errors);
		}
		return errors;
	}

	private void checkSeveralCorrect(TestQuestion question, String path, List<FieldErrorDetail> errors) {
		long correct = question.options().stream()
				.filter(option -> option != null && Boolean.TRUE.equals(option.correct()))
				.count();
		if (correct > 1 && !Boolean.TRUE.equals(question.multiple())) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
		}
	}

	private void checkOptions(List<TestOption> options, String path, List<FieldErrorDetail> errors) {
		Set<String> seen = new HashSet<>();
		for (int j = 0; j < options.size(); j++) {
			TestOption option = options.get(j);
			String optionPath = path + "[" + j + "]";
			if (option == null) {
				errors.add(new FieldErrorDetail(optionPath, FieldErrorCodes.REQUIRED));
			}
			else if (!seen.add(option.text())) {
				errors.add(new FieldErrorDetail(optionPath + ".text", FieldErrorCodes.INVALID_FORMAT));
			}
		}
	}
}
