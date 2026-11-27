package com.teachdash.items.domain.freetext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** item-free-text R1, student view. */
@JsonTest
class FreeTextItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final FreeTextItemType type = new FreeTextItemType();

	@Test
	@DisplayName("[item-free-text/R1-S7] The student view keeps the prompt and the limits and shows no limit that is absent")
	void theStudentViewKeepsThePromptAndTheLimits() {
		// given the free-text type and three payloads
		var both = new FreeTextPayload("Describe your day.", 50, 500);
		var maxOnly = new FreeTextPayload("Опишите ёлку.", null, 300);
		var none = new FreeTextPayload("Write.", null, null);

		// when studentView is called with each payload
		Object bothView = type.studentView(both);
		Object maxOnlyView = type.studentView(maxOnly);
		Object noneView = type.studentView(none);

		// then each view is an instance of the allowlist record FreeTextStudentView with exactly the fields prompt, minLength and maxLength
		for (Object view : Arrays.asList(bothView, maxOnlyView, noneView)) {
			assertThat(view).isNotNull();
			assertThat(view.getClass().getSimpleName()).isEqualTo("FreeTextStudentView");
			assertThat(view.getClass().isRecord()).isTrue();
			assertThat(Arrays.stream(view.getClass().getRecordComponents()).map(c -> c.getName()).toList())
					.containsExactly("prompt", "minLength", "maxLength");
		}
		// and they hold the prompt exactly as stored and the limits 50 and 500, no minLength and 300, and no limit
		assertThat(component(bothView, "prompt")).isEqualTo("Describe your day.");
		assertThat(component(bothView, "minLength")).isEqualTo(50);
		assertThat(component(bothView, "maxLength")).isEqualTo(500);
		assertThat(component(maxOnlyView, "prompt")).isEqualTo("Опишите ёлку.");
		assertThat(component(maxOnlyView, "minLength")).isNull();
		assertThat(component(maxOnlyView, "maxLength")).isEqualTo(300);
		assertThat(component(noneView, "prompt")).isEqualTo("Write.");
		assertThat(component(noneView, "minLength")).isNull();
		assertThat(component(noneView, "maxLength")).isNull();
	}

	@Test
	@DisplayName("[item-free-text/R1-S8] The serialised student view has exactly the allowed keys, in English and Russian")
	void theSerialisedStudentViewHasExactlyTheAllowedKeys() {
		// given the free-text type and an English and a Russian payload
		var english = new FreeTextPayload("Describe your day.", 50, 500);
		var russian = new FreeTextPayload("Опишите «ёлку».", null, 300);

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then the English JSON is an object whose keys are exactly prompt, minLength and maxLength, in this order, with the sent values
		JsonNode englishTree = mapper.readTree(englishJson);
		assertThat(englishTree.propertyNames()).containsExactly("prompt", "minLength", "maxLength");
		assertThat(englishTree.get("prompt").asString()).isEqualTo("Describe your day.");
		assertThat(englishTree.get("minLength").asInt()).isEqualTo(50);
		assertThat(englishTree.get("maxLength").asInt()).isEqualTo(500);
		// and the Russian JSON has exactly prompt and maxLength, with no minLength key and no null value anywhere
		JsonNode russianTree = mapper.readTree(russianJson);
		assertThat(russianTree.propertyNames()).containsExactly("prompt", "maxLength");
		assertThat(russianTree.get("prompt").asString()).isEqualTo("Опишите «ёлку».");
		assertThat(russianTree.get("maxLength").asInt()).isEqualTo(300);
		assertThat(russianJson).doesNotContain("null");
	}

	private static Object component(Object view, String name) {
		try {
			var component = Arrays.stream(view.getClass().getRecordComponents())
					.filter(c -> c.getName().equals(name))
					.findFirst()
					.orElseThrow();
			var accessor = component.getAccessor();
			accessor.setAccessible(true);
			return accessor.invoke(view);
		}
		catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
