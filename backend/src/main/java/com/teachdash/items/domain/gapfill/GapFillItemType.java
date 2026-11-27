package com.teachdash.items.domain.gapfill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class GapFillItemType implements ItemType<GapFillPayload> {

	private static final String TEXT = "text";

	private static final String GAP = "gap";

	private static final int MAX_GAPS = 25;

	private static final int MAX_ANSWER_LENGTH = 100;

	@Override
	public String code() {
		return "gap-fill";
	}

	@Override
	public Class<GapFillPayload> payloadClass() {
		return GapFillPayload.class;
	}

	@Override
	public Object studentView(GapFillPayload payload) {
		List<StudentSegment> segments = payload.segments().stream()
				.map(this::studentSegment)
				.toList();
		return new StudentView(segments);
	}

	private StudentSegment studentSegment(GapFillSegment segment) {
		String type = segment.type();
		if (TEXT.equals(type) && segment.text() != null) {
			return new TextView(segment.text());
		}
		if (GAP.equals(type)) {
			return new GapView();
		}
		throw new IllegalStateException("Cannot build a student view for a gap-fill segment of type " + type);
	}

	record StudentView(
			List<StudentSegment> segments) {
	}

	sealed interface StudentSegment permits TextView, GapView {
	}

	record TextView(
			String text) implements StudentSegment {

		@JsonProperty(value = "type", index = 0)
		public String type() {
			return TEXT;
		}
	}

	record GapView() implements StudentSegment {

		@JsonProperty("type")
		public String type() {
			return GAP;
		}
	}

	@Override
	public List<FieldErrorDetail> extraChecks(GapFillPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		int gaps = 0;
		boolean previousIsText = false;
		for (int i = 0; i < payload.segments().size(); i++) {
			GapFillSegment segment = payload.segments().get(i);
			String path = "segments[" + i + "]";
			if (segment == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				previousIsText = false;
				continue;
			}
			String type = segment.type();
			if (type == null || type.isBlank()) {
				errors.add(new FieldErrorDetail(path + ".type", FieldErrorCodes.REQUIRED));
			}
			else if (!TEXT.equals(type) && !GAP.equals(type)) {
				errors.add(new FieldErrorDetail(path + ".type", FieldErrorCodes.INVALID_FORMAT));
			}
			if (GAP.equals(type)) {
				gaps++;
				checkGap(segment, path, errors);
			}
			boolean isText = TEXT.equals(type);
			if (isText) {
				checkText(segment, path, previousIsText, errors);
			}
			previousIsText = isText;
		}
		if (gaps == 0) {
			errors.add(new FieldErrorDetail("segments", FieldErrorCodes.REQUIRED));
		}
		else if (gaps > MAX_GAPS) {
			errors.add(new FieldErrorDetail("segments", FieldErrorCodes.TOO_LONG));
		}
		return errors;
	}

	private void checkText(GapFillSegment segment, String path, boolean previousIsText, List<FieldErrorDetail> errors) {
		if (segment.text() == null || segment.text().isEmpty()) {
			errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.REQUIRED));
		}
		if (segment.answers() != null) {
			errors.add(new FieldErrorDetail(path + ".answers", FieldErrorCodes.UNKNOWN_FIELD));
		}
		if (previousIsText) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
		}
	}

	private void checkGap(GapFillSegment segment, String path, List<FieldErrorDetail> errors) {
		if (segment.text() != null) {
			errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.UNKNOWN_FIELD));
		}
		List<String> answers = segment.answers();
		if (answers == null || answers.isEmpty()) {
			errors.add(new FieldErrorDetail(path + ".answers", FieldErrorCodes.REQUIRED));
			return;
		}
		Set<String> seen = new HashSet<>();
		for (int j = 0; j < answers.size(); j++) {
			String answer = answers.get(j);
			String answerPath = path + ".answers[" + j + "]";
			if (answer == null || answer.isBlank()) {
				errors.add(new FieldErrorDetail(answerPath, FieldErrorCodes.REQUIRED));
			}
			else if (answer.length() > MAX_ANSWER_LENGTH) {
				errors.add(new FieldErrorDetail(answerPath, FieldErrorCodes.TOO_LONG));
			}
			else if (!seen.add(answer)) {
				errors.add(new FieldErrorDetail(answerPath, FieldErrorCodes.INVALID_FORMAT));
			}
		}
	}
}
