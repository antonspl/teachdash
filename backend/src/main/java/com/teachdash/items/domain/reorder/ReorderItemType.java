package com.teachdash.items.domain.reorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemType;

@Component
public class ReorderItemType implements ItemType<ReorderPayload> {

	private static final Map<String, Integer> MAX_UNIT_LENGTH_BY_MODE = Map.of(
			"words", 100,
			"sentences", 500,
			"letters", 4);

	private static final int MAX_TOTAL_UNITS = 200;

	private static final int MAX_HINT_LENGTH = 100;

	@Override
	public String code() {
		return "reorder";
	}

	@Override
	public Class<ReorderPayload> payloadClass() {
		return ReorderPayload.class;
	}

	@Override
	public Object studentView(ReorderPayload payload) {
		if (!isKnownMode(payload.mode())) {
			throw new IllegalStateException("Cannot build a student view for a reorder payload with an invalid mode");
		}
		if (payload.entries() == null) {
			throw new IllegalStateException("Cannot build a student view for a reorder payload without entries");
		}
		List<StudentEntry> entries = new ArrayList<>();
		for (ReorderEntry entry : payload.entries()) {
			requireComplete(entry);
			entries.add(new StudentEntry(shuffled(entry.units()), entry.hint()));
		}
		return new StudentView(payload.mode(), entries);
	}

	private void requireComplete(ReorderEntry entry) {
		if (entry == null
				|| entry.units() == null
				|| entry.units().stream().anyMatch(Objects::isNull)
				|| !hasTwoDistinctUnits(entry.units())) {
			throw new IllegalStateException("Cannot build a student view for an incomplete reorder entry");
		}
	}

	private boolean isKnownMode(String mode) {
		return mode != null && MAX_UNIT_LENGTH_BY_MODE.containsKey(mode);
	}

	private boolean hasTwoDistinctUnits(List<String> units) {
		return new HashSet<>(units).size() >= ReorderEntry.MIN_UNITS;
	}

	/**
	 * The sort makes the shuffle input canonical; the seed depends on the stored order. A shuffle equal to the
	 * stored order is rotated, so the correct order is never shown (the units are not all equal).
	 */
	private List<String> shuffled(List<String> units) {
		List<String> shuffled = new ArrayList<>(units);
		Collections.sort(shuffled);
		long seed = String.join("\u0000", units).hashCode();
		Collections.shuffle(shuffled, new Random(seed));
		if (shuffled.equals(units)) {
			Collections.rotate(shuffled, -1);
		}
		return shuffled;
	}

	@Override
	public List<FieldErrorDetail> extraChecks(ReorderPayload payload) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		Integer maxUnitLength = checkMode(payload.mode(), errors);
		int totalUnits = 0;
		for (int i = 0; i < payload.entries().size(); i++) {
			ReorderEntry entry = payload.entries().get(i);
			String path = "entries[" + i + "]";
			if (entry == null) {
				errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
				continue;
			}
			totalUnits += entry.units().size();
			checkUnits(entry.units(), path + ".units", maxUnitLength, errors);
			checkHint(entry.hint(), path + ".hint", errors);
		}
		if (totalUnits > MAX_TOTAL_UNITS) {
			errors.add(new FieldErrorDetail("entries", FieldErrorCodes.TOO_LONG));
		}
		return errors;
	}

	/** Returns the unit length limit of the mode, or null when the mode is not valid. */
	private Integer checkMode(String mode, List<FieldErrorDetail> errors) {
		if (mode == null || mode.isBlank()) {
			errors.add(new FieldErrorDetail("mode", FieldErrorCodes.REQUIRED));
			return null;
		}
		if (!isKnownMode(mode)) {
			errors.add(new FieldErrorDetail("mode", FieldErrorCodes.INVALID_FORMAT));
			return null;
		}
		return MAX_UNIT_LENGTH_BY_MODE.get(mode);
	}

	/** The distinct-unit rule is skipped for an entry with a unit that has its own error. */
	private void checkUnits(List<String> units, String path, Integer maxUnitLength, List<FieldErrorDetail> errors) {
		boolean unitHasError = false;
		for (int j = 0; j < units.size(); j++) {
			String unit = units.get(j);
			String unitPath = path + "[" + j + "]";
			if (unit == null || unit.isBlank()) {
				errors.add(new FieldErrorDetail(unitPath, FieldErrorCodes.REQUIRED));
				unitHasError = true;
			}
			else if (maxUnitLength != null && unit.length() > maxUnitLength) {
				errors.add(new FieldErrorDetail(unitPath, FieldErrorCodes.TOO_LONG));
				unitHasError = true;
			}
		}
		if (!unitHasError && !hasTwoDistinctUnits(units)) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.INVALID_FORMAT));
		}
	}

	private void checkHint(String hint, String path, List<FieldErrorDetail> errors) {
		if (hint == null) {
			return;
		}
		if (hint.isBlank()) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.REQUIRED));
		}
		else if (hint.length() > MAX_HINT_LENGTH) {
			errors.add(new FieldErrorDetail(path, FieldErrorCodes.TOO_LONG));
		}
	}

	record StudentView(
			String mode,
			List<StudentEntry> entries) {
	}

	record StudentEntry(
			List<String> units,
			@JsonInclude(JsonInclude.Include.NON_NULL) String hint) {
	}
}
