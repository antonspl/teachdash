package com.teachdash.items.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.teachdash.support.ApiTest;

class ItemOwnerConstraintTest extends ApiTest {

	@Test
	@DisplayName("The database refuses an item whose owner differs from its part's owner")
	void itemOwnerMustMatchPartOwner() {
		// given
		UUID teacherA = createTeacher("a@example.com", "password-a-1");
		UUID teacherB = createTeacher("b@example.com", "password-b-1");
		UUID course = createCourse(teacherA, "Course", "en", "ru");
		UUID lesson = createLesson(teacherA, course, "Lesson", 1);
		UUID part = createPart(teacherA, lesson, "Part", 1);

		// when / then
		assertThatThrownBy(() -> createItem(teacherB, part, "test-note", "{\"text\":\"x\"}", 1))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
