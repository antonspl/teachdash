package com.teachdash.items.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.items.support.TestNoteType;

class ItemTypeRegistryTest {

	@Test
	@DisplayName("Registry fails on a duplicate type code and names the code")
	void duplicateCodeFails() {
		// given
		List<ItemType> types = List.of(new TestNoteType(), new TestNoteType());

		// when / then
		assertThatThrownBy(() -> new ItemTypeRegistry(types))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("test-note");
	}

	@Test
	@DisplayName("Registry finds a registered type by its code")
	void findsByCode() {
		// given
		TestNoteType note = new TestNoteType();

		// when
		ItemTypeRegistry registry = new ItemTypeRegistry(List.of(note));

		// then
		assertThat(registry.find("test-note")).containsSame(note);
		assertThat(registry.find("unknown")).isEmpty();
	}
}
