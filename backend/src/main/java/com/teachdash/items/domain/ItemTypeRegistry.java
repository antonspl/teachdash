package com.teachdash.items.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ItemTypeRegistry {

	private final Map<String, ItemType<?>> byCode;

	public ItemTypeRegistry(List<ItemType<?>> types) {
		Map<String, ItemType<?>> map = new HashMap<>();
		for (ItemType<?> type : types) {
			if (map.putIfAbsent(type.code(), type) != null) {
				throw new IllegalStateException("Duplicate item type code: " + type.code());
			}
		}
		this.byCode = Map.copyOf(map);
	}

	public Optional<ItemType<?>> find(String code) {
		return Optional.ofNullable(byCode.get(code));
	}
}
