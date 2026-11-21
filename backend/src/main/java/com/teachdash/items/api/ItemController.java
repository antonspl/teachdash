package com.teachdash.items.api;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.teachdash.items.domain.ItemService;
import com.teachdash.items.domain.ItemType;
import com.teachdash.items.domain.ItemTypeRegistry;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@RestController
class ItemController {

	private static final int MAX_PAYLOAD_BYTES = 65536;

	private final ItemService items;

	private final ItemTypeRegistry registry;

	private final ObjectMapper mapper;

	ItemController(ItemService items, ItemTypeRegistry registry, ObjectMapper mapper) {
		this.items = items;
		this.registry = registry;
		this.mapper = mapper;
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
		ItemType type = checkBody(request);
		String payloadJson = checkPayload(request.payload(), type);
		return ItemResponse.from(items.create(teacherId, courseId, lessonId, partId, type, payloadJson));
	}

	/** Steps 1 and 2 of the payload rules are reported together; steps 3 to 5 run only if both passed. */
	private ItemType checkBody(CreateItemRequest request) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		ItemType type = null;
		if (request.type() == null || request.type().isBlank()) {
			errors.add(new FieldErrorDetail("type", FieldErrorCodes.REQUIRED));
		} else {
			Optional<ItemType> found = registry.find(request.type());
			if (found.isPresent()) {
				type = found.get();
			} else {
				errors.add(new FieldErrorDetail("type", FieldErrorCodes.UNKNOWN_TYPE));
			}
		}
		JsonNode payload = request.payload();
		if (payload == null || payload.isNull()) {
			errors.add(new FieldErrorDetail("payload", FieldErrorCodes.REQUIRED));
		} else if (!payload.isObject()) {
			errors.add(new FieldErrorDetail("payload", FieldErrorCodes.INVALID_TYPE));
		}
		if (!errors.isEmpty()) {
			throw ApiException.fields(errors);
		}
		return type;
	}

	/** Returns the compact JSON of the normalised payload: the exact string whose size was checked and that is stored. */
	private String checkPayload(JsonNode node, ItemType type) {
		ObjectNode payload = (ObjectNode) node;
		requireWithinLimit(mapper.writeValueAsBytes(payload));
		if (isNotStorable(payload)) {
			throw ApiException.fields(List.of(new FieldErrorDetail("payload", FieldErrorCodes.INVALID_FORMAT)));
		}
		byte[] normalised = mapper.writeValueAsBytes(type.validate(payload));
		requireWithinLimit(normalised);
		return new String(normalised, StandardCharsets.UTF_8);
	}

	private static void requireWithinLimit(byte[] json) {
		if (json.length > MAX_PAYLOAD_BYTES) {
			throw ApiException.fields(List.of(new FieldErrorDetail("payload", FieldErrorCodes.TOO_LONG)));
		}
	}

	/**
	 * Jackson's node deserializers bypass the NUL-rejecting String deserializer, and jsonb cannot store U+0000,
	 * an unpaired surrogate or a number outside the double range without loss or an error.
	 */
	private static boolean isNotStorable(JsonNode node) {
		if (node.isString()) {
			return isNotStorable(node.stringValue());
		}
		if (node.isFloatingPointNumber()) {
			return !Double.isFinite(node.doubleValue());
		}
		if (node.isObject()) {
			for (var field : node.properties()) {
				if (isNotStorable(field.getKey()) || isNotStorable(field.getValue())) {
					return true;
				}
			}
		} else if (node.isArray()) {
			for (JsonNode element : node) {
				if (isNotStorable(element)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isNotStorable(String text) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\u0000') {
				return true;
			}
			if (Character.isHighSurrogate(c)) {
				if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
					return true;
				}
				i++;
			} else if (Character.isLowSurrogate(c)) {
				return true;
			}
		}
		return false;
	}
}
