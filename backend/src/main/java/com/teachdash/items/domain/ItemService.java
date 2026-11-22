package com.teachdash.items.domain;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.items.persistence.ItemRepository;
import com.teachdash.parts.persistence.PartRepository;

@Service
public class ItemService {

	private final UserRepository users;

	private final PartRepository parts;

	private final ItemRepository items;

	private final ItemTypeRegistry registry;

	private final ItemPayloadValidator validator;

	private final Clock clock;

	private final int maxPerPart;

	ItemService(
			UserRepository users,
			PartRepository parts,
			ItemRepository items,
			ItemTypeRegistry registry,
			ItemPayloadValidator validator,
			Clock clock,
			@Value("${teachdash.items.max-per-part}") int maxPerPart) {
		this.users = users;
		this.parts = parts;
		this.items = items;
		this.registry = registry;
		this.validator = validator;
		this.clock = clock;
		this.maxPerPart = maxPerPart;
	}

	@Transactional
	public Item create(
			UUID teacherId,
			UUID courseId,
			UUID lessonId,
			UUID partId,
			String type,
			String payloadJson) {
		lockForWrite(teacherId, courseId, lessonId, partId);
		if (items.countInPart(partId, teacherId) >= maxPerPart) {
			throw new ApiException(HttpStatus.CONFLICT, "ITEM_LIMIT_REACHED");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		UUID id = UUID.randomUUID();
		ItemRepository.Stored stored = items.insertAtEnd(id, teacherId, partId, type, payloadJson, now);
		return new Item(id, type, stored.payload(), stored.position(), 1, now, now);
	}

	@Transactional
	public Item move(
			UUID teacherId,
			UUID courseId,
			UUID lessonId,
			UUID partId,
			UUID itemId,
			BigInteger requested) {
		lockForWrite(teacherId, courseId, lessonId, partId);
		int old = items.findInPart(itemId, partId, teacherId)
				.orElseThrow(ApiException::notFound)
				.position();
		int count = items.countInPart(partId, teacherId);
		int target = requested.max(BigInteger.ONE).min(BigInteger.valueOf(count)).intValueExact();
		if (target > old) {
			items.shiftRange(partId, teacherId, old + 1, target, -1);
		}
		else if (target < old) {
			items.shiftRange(partId, teacherId, target, old - 1, 1);
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		return items.setPosition(itemId, partId, teacherId, target, now);
	}

	@Transactional
	public void delete(UUID teacherId, UUID courseId, UUID lessonId, UUID partId, UUID itemId) {
		lockForWrite(teacherId, courseId, lessonId, partId);
		int old = items.deleteReturningPosition(itemId, partId, teacherId).orElseThrow(ApiException::notFound);
		items.closeGap(partId, teacherId, old);
	}

	private void lockForWrite(UUID teacherId, UUID courseId, UUID lessonId, UUID partId) {
		// The user row is locked first, as in CourseService.rename, to avoid a deadlock.
		// Then course, lesson and part, the same order as PartService extended by the part.
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!parts.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!parts.lockLesson(lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!items.lockPart(partId, lessonId, teacherId)) {
			throw ApiException.notFound();
		}
	}

	@Transactional(readOnly = true)
	public List<Item> list(UUID teacherId, UUID courseId, UUID lessonId, UUID partId) {
		if (!items.partExists(partId, lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
		return items.findByPart(partId, teacherId);
	}

	@Transactional
	public Item edit(
			UUID teacherId,
			UUID courseId,
			UUID lessonId,
			UUID partId,
			UUID itemId,
			long version,
			Map<String, Object> payload) {
		users.findActiveByIdForUpdate(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
		if (!parts.lockCourse(courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!parts.lockLesson(lessonId, courseId, teacherId)) {
			throw ApiException.notFound();
		}
		if (!items.lockPart(partId, lessonId, teacherId)) {
			throw ApiException.notFound();
		}
		ItemRepository.Locked locked = items.lockItem(itemId, partId, teacherId)
				.orElseThrow(ApiException::notFound);
		ItemType<?> type = registry.find(locked.type()).orElseThrow();
		String payloadJson = validator.toStoredJson(validator.validate(type, payload));
		if (locked.version() != version) {
			throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		return items.updatePayload(itemId, partId, teacherId, payloadJson, now);
	}
}
