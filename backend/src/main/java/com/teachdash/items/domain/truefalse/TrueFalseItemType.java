package com.teachdash.items.domain.truefalse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class TrueFalseItemType implements ItemType<TrueFalsePayload> {

	private static final String TRUE = "true";

	private static final String FALSE = "false";

	private static final String NOT_STATED = "not-stated";

	@Override
	public String code() {
		return "true-false";
	}

	@Override
	public Class<TrueFalsePayload> payloadClass() {
		return TrueFalsePayload.class;
	}

	/** Built field by field, so that a field added to the stored record never reaches a student by default. */
	@Override
	public Object studentView(TrueFalsePayload payload) {
		if (payload.statements() == null) {
			throw new IllegalStateException("Cannot build a student view of a true-false item without statements");
		}
		List<StatementView> statements = payload.statements().stream()
				.map(this::statementView)
				.toList();
		return new StudentView(Boolean.TRUE.equals(payload.allowNotStated()), statements);
	}

	private StatementView statementView(TrueFalseStatement statement) {
		if (statement == null || statement.text() == null) {
			throw new IllegalStateException("Cannot build a student view of a true-false statement without text");
		}
		return new StatementView(statement.text());
	}

	@Override
	public List<FieldErrorDetail> extraChecks(TrueFalsePayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		boolean allowNotStated = Boolean.TRUE.equals(payload.allowNotStated());
		Set<String> seenTexts = new HashSet<>();
		for (int i = 0; i < payload.statements().size(); i++) {
			TrueFalseStatement statement = payload.statements().get(i);
			String path = "statements[" + i + "]";
			if (statement == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				continue;
			}
			if (!isAllowedAnswer(statement.answer(), allowNotStated)) {
				errors.add(new FieldErrorDetail(path + ".answer", FieldErrorCodes.INVALID_FORMAT));
			}
			if (!seenTexts.add(statement.text())) {
				errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.INVALID_FORMAT));
			}
		}
		return errors;
	}

	private boolean isAllowedAnswer(String answer, boolean allowNotStated) {
		return TRUE.equals(answer) || FALSE.equals(answer) || (allowNotStated && NOT_STATED.equals(answer));
	}

	record StudentView(
			boolean allowNotStated,
			List<StatementView> statements) {
	}

	record StatementView(
			String text) {
	}
}
