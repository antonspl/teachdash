package com.teachdash.items.domain.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class MatchingItemType implements ItemType<MatchingPayload> {

	private static final int MAX_VALUE_LENGTH = 100;

	@Override
	public String code() {
		return "matching";
	}

	@Override
	public Class<MatchingPayload> payloadClass() {
		return MatchingPayload.class;
	}

	@Override
	public Object studentView(MatchingPayload payload) {
		if (payload.pairs() == null) {
			throw new IllegalStateException("Cannot build a student view for a matching payload without pairs");
		}
		List<String> lefts = new ArrayList<>();
		List<String> rights = new ArrayList<>();
		for (MatchingPair pair : payload.pairs()) {
			requireComplete(pair);
			lefts.add(pair.left());
			rights.add(pair.right());
		}
		return new StudentView(lefts, shuffledRights(lefts, rights));
	}

	private void requireComplete(MatchingPair pair) {
		if (pair == null || pair.left() == null || pair.right() == null) {
			throw new IllegalStateException("Cannot build a student view for an incomplete matching pair");
		}
	}

	/**
	 * The seed depends only on the sorted values, not on which right belongs to which left, so every pairing of the
	 * same values gives the same view. If the shuffle leaves the stored pairing, the list is rotated.
	 */
	private List<String> shuffledRights(List<String> lefts, List<String> rights) {
		List<String> shuffled = sorted(rights);
		Collections.shuffle(shuffled, new Random(seed(lefts, rights)));
		if (shuffled.size() > 1 && shuffled.equals(rights)) {
			Collections.rotate(shuffled, -1);
		}
		return shuffled;
	}

	private long seed(List<String> lefts, List<String> rights) {
		List<String> values = sorted(lefts);
		values.addAll(sorted(rights));
		return String.join("\u0000", values).hashCode();
	}

	private List<String> sorted(List<String> values) {
		List<String> copy = new ArrayList<>(values);
		Collections.sort(copy);
		return copy;
	}

	@Override
	public List<FieldErrorDetail> extraChecks(MatchingPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		Set<String> seenLefts = new HashSet<>();
		Set<String> seenRights = new HashSet<>();
		for (int i = 0; i < payload.pairs().size(); i++) {
			MatchingPair pair = payload.pairs().get(i);
			String path = "pairs[" + i + "]";
			if (pair == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				continue;
			}
			checkValue(pair.left(), path + ".left", seenLefts, errors);
			checkValue(pair.right(), path + ".right", seenRights, errors);
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
			List<String> left,
			List<String> right) {
	}
}
