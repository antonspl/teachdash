package com.teachdash.items.domain;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public interface ItemType {

	String code();

	int schemaVersion();

	/** Returns the normalised payload to store, or throws field errors under {@code payload.}. */
	ObjectNode validate(ObjectNode payload);

	/** Builds a new tree from the fields a student may see; never returns the stored tree. */
	JsonNode studentProjection(ObjectNode payload);
}
