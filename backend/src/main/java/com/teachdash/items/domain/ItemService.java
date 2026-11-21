package com.teachdash.items.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.items.persistence.ItemRepository;
import com.teachdash.parts.persistence.PartRepository;

import tools.jackson.databind.node.ObjectNode;

@Service
public class ItemService {

	private final UserRepository users;

	private final PartRepository parts;

	private final ItemRepository items;

	private final Clock clock;

	private final int maxPerPart;

	ItemService(
			UserRepository users,
			PartRepository parts,
			ItemRepository items,
			Clock clock,
			@Value("${teachdash.items.max-per-part}") int maxPerPart) {
		this.users = users;
		this.parts = parts;
		this.items = items;
		this.clock = clock;
		this.maxPerPart = maxPerPart;
	}

	@Transactional
	public Item create(
			UUID teacherId,
			UUID courseId,
			UUID lessonId,
			UUID partId,
			ItemType type,
			ObjectNode payload) {
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
		if (items.countInPart(partId) >= maxPerPart) {
			throw new ApiException(HttpStatus.CONFLICT, "ITEM_LIMIT_REACHED");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
		UUID id = UUID.randomUUID();
		ItemRepository.Stored stored = items.insertAtEnd(
				id, teacherId, partId, type.code(), payload, type.schemaVersion(), now);
		return new Item(id, type.code(), stored.payload(), stored.position(), 1, now, now);
	}
}
