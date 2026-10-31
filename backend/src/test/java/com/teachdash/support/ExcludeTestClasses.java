package com.teachdash.support;

import java.io.IOException;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * Keeps classes from the test output directory out of component scanning. {@code @SpringBootTest}
 * does this itself; an application started with {@code SpringApplicationBuilder} inside a test does
 * not, and would pick up test configuration (e.g. the movable clock). Register an instance as a
 * singleton before refresh so that application starts like production.
 */
public class ExcludeTestClasses extends TypeExcludeFilter {

	@Override
	public boolean match(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
		String location = reader.getResource().getURL().toString();
		return location.contains("/classes/java/test/") || location.contains("/test-classes/")
				|| location.contains("/out/test/");
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ExcludeTestClasses;
	}

	@Override
	public int hashCode() {
		return ExcludeTestClasses.class.hashCode();
	}
}
