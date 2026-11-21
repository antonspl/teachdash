package com.teachdash.items.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ItemTypeRegistry {

	public ItemTypeRegistry(List<ItemType> types) {
	}

	public Optional<ItemType> find(String code) {
		return Optional.empty();
	}
}
