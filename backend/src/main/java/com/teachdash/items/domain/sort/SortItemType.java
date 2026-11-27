package com.teachdash.items.domain.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class SortItemType implements ItemType<SortPayload> {

	private static final int MAX_VALUE_LENGTH = 100;

	private static final int MAX_TOTAL_WORDS = 50;

	@Override
	public String code() {
		return "sort";
	}

	@Override
	public Class<SortPayload> payloadClass() {
		return SortPayload.class;
	}

	@Override
	public Object studentView(SortPayload payload) {
		if (payload.columns() == null) {
			throw new IllegalStateException("Cannot build a student view for a sort payload without columns");
		}
		List<String> titles = new ArrayList<>();
		List<String> words = new ArrayList<>();
		for (SortColumn column : payload.columns()) {
			requireComplete(column);
			titles.add(column.title());
			words.addAll(column.words());
		}
		return new StudentView(titles, shuffledWords(words));
	}

	private void requireComplete(SortColumn column) {
		if (column == null || column.title() == null || column.words() == null || hasNullWord(column)) {
			throw new IllegalStateException("Cannot build a student view for an incomplete sort column");
		}
	}

	private boolean hasNullWord(SortColumn column) {
		return column.words().stream().anyMatch(Objects::isNull);
	}

	/** Sorting makes the shuffle input and the seed independent of which column holds which word. */
	private List<String> shuffledWords(List<String> storedWords) {
		List<String> shuffled = new ArrayList<>(storedWords);
		Collections.sort(shuffled);
		long seed = String.join("\u0000", shuffled).hashCode();
		Collections.shuffle(shuffled, new Random(seed));
		return shuffled;
	}

	@Override
	public List<FieldErrorDetail> extraChecks(SortPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		Set<String> seenTitles = new HashSet<>();
		Set<String> seenWords = new HashSet<>();
		int totalWords = 0;
		for (int i = 0; i < payload.columns().size(); i++) {
			SortColumn column = payload.columns().get(i);
			String path = "columns[" + i + "]";
			if (column == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				continue;
			}
			checkValue(column.title(), path + ".title", seenTitles, errors);
			for (int j = 0; j < column.words().size(); j++) {
				checkValue(column.words().get(j), path + ".words[" + j + "]", seenWords, errors);
			}
			totalWords += column.words().size();
		}
		if (totalWords > MAX_TOTAL_WORDS) {
			errors.add(new FieldErrorDetail("columns", FieldErrorCodes.TOO_LONG));
		}
		return errors;
	}

	/** A value with its own error is not added to the seen set, so it is not compared. */
	private void checkValue(String value, String path, Set<String> seen, List<FieldErrorDetail> errors) {
		if (value == null || value.isBlank()) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
		}
		else if (value.length() > MAX_VALUE_LENGTH) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.TOO_LONG));
		}
		else if (!seen.add(value)) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
		}
	}

	record StudentView(
			List<String> titles,
			List<String> words) {
	}
}
