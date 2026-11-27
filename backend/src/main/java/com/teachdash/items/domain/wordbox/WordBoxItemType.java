package com.teachdash.items.domain.wordbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class WordBoxItemType implements ItemType<WordBoxPayload> {

	private static final String TEXT = "text";

	private static final String GAP = "gap";

	private static final int MAX_GAPS = 25;

	private static final int MAX_WORD_LENGTH = 100;

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
		List<StudentSegment> segments = payload.segments().stream()
				.map(this::studentSegment)
				.toList();
		return new StudentView(segments, wordBox(payload));
	}

	private StudentSegment studentSegment(WordBoxSegment segment) {
		if (segment == null) {
			throw new IllegalStateException("Cannot build a student view for a null word-box segment");
		}
		String type = segment.type();
		if (TEXT.equals(type) && segment.text() != null) {
			return new TextView(segment.text());
		}
		if (GAP.equals(type) && segment.answer() != null) {
			return new GapView();
		}
		throw new IllegalStateException("Cannot build a student view for a word-box segment of type " + type);
	}

	private List<String> wordBox(WordBoxPayload payload) {
		List<String> words = new ArrayList<>();
		payload.segments().stream()
				.filter(segment -> GAP.equals(segment.type()))
				.forEach(segment -> words.add(segment.answer()));
		if (payload.extraWords() != null) {
			words.addAll(payload.extraWords());
		}
		Collections.sort(words);
		long seed = String.join("\u0000", words).hashCode();
		Collections.shuffle(words, new Random(seed));
		return words;
	}

	record StudentView(
			List<StudentSegment> segments,
			List<String> wordBox) {
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
	public List<FieldErrorDetail> extraChecks(WordBoxPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		int gaps = 0;
		boolean previousIsText = false;
		for (int i = 0; i < payload.segments().size(); i++) {
			WordBoxSegment segment = payload.segments().get(i);
			String path = "segments[" + i + "]";
			if (segment == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				previousIsText = false;
				continue;
			}
			String type = segment.type();
			checkType(type, path, errors);
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
		checkGapCount(gaps, errors);
		checkExtraWords(payload, errors);
		return errors;
	}

	private void checkType(String type, String path, List<FieldErrorDetail> errors) {
		if (type == null || type.isBlank()) {
			errors.add(new FieldErrorDetail(path + ".type", FieldErrorCodes.REQUIRED));
		}
		else if (!TEXT.equals(type) && !GAP.equals(type)) {
			errors.add(new FieldErrorDetail(path + ".type", FieldErrorCodes.INVALID_FORMAT));
		}
	}

	private void checkGapCount(int gaps, List<FieldErrorDetail> errors) {
		if (gaps == 0) {
			errors.add(new FieldErrorDetail("segments", FieldErrorCodes.REQUIRED));
		}
		else if (gaps > MAX_GAPS) {
			errors.add(new FieldErrorDetail("segments", FieldErrorCodes.TOO_LONG));
		}
	}

	private void checkText(WordBoxSegment segment, String path, boolean previousIsText, List<FieldErrorDetail> errors) {
		if (segment.text() == null || segment.text().isEmpty()) {
			errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.REQUIRED));
		}
		if (segment.answer() != null) {
			errors.add(new FieldErrorDetail(path + ".answer", FieldErrorCodes.UNKNOWN_FIELD));
		}
		if (previousIsText) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
		}
	}

	private void checkGap(WordBoxSegment segment, String path, List<FieldErrorDetail> errors) {
		if (segment.text() != null) {
			errors.add(new FieldErrorDetail(path + ".text", FieldErrorCodes.UNKNOWN_FIELD));
		}
		checkWord(segment.answer(), path + ".answer", errors);
	}

	private void checkWord(String word, String path, List<FieldErrorDetail> errors) {
		if (word == null || word.isBlank()) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
		}
		else if (word.length() > MAX_WORD_LENGTH) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.TOO_LONG));
		}
	}

	private void checkExtraWords(WordBoxPayload payload, List<FieldErrorDetail> errors) {
		if (payload.extraWords() == null) {
			return;
		}
		Set<String> taken = answers(payload);
		for (int i = 0; i < payload.extraWords().size(); i++) {
			String word = payload.extraWords().get(i);
			String path = "extraWords[" + i + "]";
			int before = errors.size();
			checkWord(word, path, errors);
			if (errors.size() == before && !taken.add(word)) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
			}
		}
	}

	private Set<String> answers(WordBoxPayload payload) {
		Set<String> answers = new HashSet<>();
		for (WordBoxSegment segment : payload.segments()) {
			if (segment != null && GAP.equals(segment.type()) && segment.answer() != null) {
				answers.add(segment.answer());
			}
		}
		return answers;
	}
}
