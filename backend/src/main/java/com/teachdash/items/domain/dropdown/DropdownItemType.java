package com.teachdash.items.domain.dropdown;

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
public class DropdownItemType implements ItemType<DropdownPayload> {

	private static final String TEXT = "text";

	private static final String GAP = "gap";

	private static final int MAX_GAPS = 25;

	private static final int MAX_OPTION_LENGTH = 100;

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
		List<StudentSegment> segments = payload.segments().stream()
				.map(this::studentSegment)
				.toList();
		return new StudentView(segments);
	}

	private StudentSegment studentSegment(DropdownSegment segment) {
		String type = segment.type();
		if (TEXT.equals(type) && segment.text() != null) {
			return new TextView(segment.text());
		}
		if (GAP.equals(type) && segment.options() != null) {
			return new GapView(List.copyOf(segment.options()));
		}
		throw new IllegalStateException("Cannot build a student view for a dropdown segment of type " + type);
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

	record GapView(
			List<String> options) implements StudentSegment {

		@JsonProperty(value = "type", index = 0)
		public String type() {
			return GAP;
		}
	}

	@Override
	public List<FieldErrorDetail> extraChecks(DropdownPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		int gaps = 0;
		boolean previousIsText = false;
		for (int i = 0; i < payload.segments().size(); i++) {
			DropdownSegment segment = payload.segments().get(i);
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

	private void checkText(DropdownSegment segment, String path, boolean previousIsText, List<FieldErrorDetail> errors) {
		if (segment.text() == null || segment.text().isEmpty()) {
			errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.REQUIRED));
		}
		if (segment.options() != null) {
			errors.add(new FieldErrorDetail(path + ".options", FieldErrorCodes.UNKNOWN_FIELD));
		}
		if (segment.correct() != null) {
			errors.add(new FieldErrorDetail(path + ".correct", FieldErrorCodes.UNKNOWN_FIELD));
		}
		if (previousIsText) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
		}
	}

	private void checkGap(DropdownSegment segment, String path, List<FieldErrorDetail> errors) {
		if (segment.text() != null) {
			errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.UNKNOWN_FIELD));
		}
		boolean optionsValid = checkOptions(segment.options(), path, errors);
		Integer correct = segment.correct();
		if (correct == null) {
			errors.add(new FieldErrorDetail(path + ".correct", FieldErrorCodes.REQUIRED));
		}
		else if (optionsValid && (correct < 0 || correct >= segment.options().size())) {
			errors.add(new FieldErrorDetail(path + ".correct", FieldErrorCodes.INVALID_FORMAT));
		}
	}

	/** Returns true when the options are present and have no error. */
	private boolean checkOptions(List<String> options, String path, List<FieldErrorDetail> errors) {
		if (options == null) {
			errors.add(new FieldErrorDetail(path + ".options", FieldErrorCodes.REQUIRED));
			return false;
		}
		boolean valid = true;
		Set<String> seen = new HashSet<>();
		for (int j = 0; j < options.size(); j++) {
			String option = options.get(j);
			String optionPath = path + ".options[" + j + "]";
			if (option == null || option.isBlank()) {
				errors.add(new FieldErrorDetail(optionPath, FieldErrorCodes.REQUIRED));
				valid = false;
			}
			else if (option.length() > MAX_OPTION_LENGTH) {
				errors.add(new FieldErrorDetail(optionPath, FieldErrorCodes.TOO_LONG));
				valid = false;
			}
			else if (!seen.add(option)) {
				errors.add(new FieldErrorDetail(optionPath, FieldErrorCodes.INVALID_FORMAT));
				valid = false;
			}
		}
		return valid;
	}
}
