package com.teachdash.items.api;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.common.ApiException;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.api.dto.CreateItemRequest;
import com.teachdash.items.api.dto.ItemResponse;
import com.teachdash.items.domain.ItemPayloadValidator;
import com.teachdash.items.domain.ItemService;
import com.teachdash.items.domain.ItemType;
import com.teachdash.items.domain.ItemTypeRegistry;

@RestController
class ItemController {

	private final ItemService items;

	private final ItemTypeRegistry registry;

	private final ItemPayloadValidator validator;

	ItemController(ItemService items, ItemTypeRegistry registry, ItemPayloadValidator validator) {
		this.items = items;
		this.registry = registry;
		this.validator = validator;
	}

	@PostMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items")
	@ResponseStatus(HttpStatus.CREATED)
	ItemResponse create(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			@RequestBody CreateItemRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		ItemType<?> type = validateEnvelope(request);
		String payloadJson = validator.toStoredJson(validator.validate(type, request.payload()));
		return ItemResponse.from(items.create(teacherId, courseId, lessonId, partId, type.code(), payloadJson));
	}

	private ItemType<?> validateEnvelope(CreateItemRequest request) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		ItemType<?> type = null;
		if (request.type() == null || request.type().isBlank()) {
			errors.add(new FieldErrorDetail("type", FieldErrorCodes.REQUIRED));
		}
		else {
			type = registry.find(request.type()).orElse(null);
			if (type == null) {
				errors.add(new FieldErrorDetail("type", FieldErrorCodes.UNKNOWN_TYPE));
			}
		}
		if (request.payload() == null) {
			errors.add(new FieldErrorDetail("payload", FieldErrorCodes.REQUIRED));
		}
		if (!errors.isEmpty()) {
			throw ApiException.fields(errors);
		}
		return type;
	}
}
