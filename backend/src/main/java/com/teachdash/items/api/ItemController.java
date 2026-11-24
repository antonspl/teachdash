package com.teachdash.items.api;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.teachdash.common.ApiException;
import com.teachdash.common.FieldErrorCodes;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.api.dto.CreateItemRequest;
import com.teachdash.items.api.dto.EditItemRequest;
import com.teachdash.items.api.dto.ItemResponse;
import com.teachdash.items.api.dto.MoveItemRequest;
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
		return ItemResponse.from(items.create(teacherId, courseId, lessonId, partId, type.code(), request.title(), payloadJson));
	}

	@PatchMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}/position")
	ItemResponse move(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			@PathVariable UUID itemId,
			@Valid @RequestBody MoveItemRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return ItemResponse.from(items.move(teacherId, courseId, lessonId, partId, itemId, request.position()));
	}

	@DeleteMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			@PathVariable UUID itemId,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		items.delete(teacherId, courseId, lessonId, partId, itemId);
	}

	@GetMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items")
	List<ItemResponse> list(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return items.list(teacherId, courseId, lessonId, partId).stream()
				.map(ItemResponse::from)
				.toList();
	}

	@PatchMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}")
	ItemResponse edit(
			@PathVariable UUID courseId,
			@PathVariable UUID lessonId,
			@PathVariable UUID partId,
			@PathVariable UUID itemId,
			@RequestBody EditItemRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		validateEditEnvelope(request);
		return ItemResponse.from(items.edit(
				teacherId, courseId, lessonId, partId, itemId, request.version().longValueExact(), request.title(), request.payload()));
	}

	private void validateEditEnvelope(EditItemRequest request) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		if (request.payload() == null) {
			errors.add(new FieldErrorDetail("payload", FieldErrorCodes.REQUIRED));
		}
		if (request.version() == null) {
			errors.add(new FieldErrorDetail("version", FieldErrorCodes.REQUIRED));
		}
		else if (request.version().bitLength() > 63) {
			errors.add(new FieldErrorDetail("version", FieldErrorCodes.INVALID_TYPE));
		}
		if (!errors.isEmpty()) {
			throw ApiException.fields(errors);
		}
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
