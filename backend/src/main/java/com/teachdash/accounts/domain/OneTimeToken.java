package com.teachdash.accounts.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/** Only the hash of the token is stored (design D5). */
@Table("one_time_tokens")
public class OneTimeToken implements Persistable<String> {

	@Id
	private final String tokenHash;
	private final UUID userId;
	private final TokenPurpose purpose;
	private final Instant expiresAt;
	private final Instant usedAt;
	private final Instant createdAt;

	@Transient
	private boolean newEntity;

	@PersistenceCreator
	OneTimeToken(String tokenHash, UUID userId, TokenPurpose purpose, Instant expiresAt, Instant usedAt,
			Instant createdAt) {
		this.tokenHash = tokenHash;
		this.userId = userId;
		this.purpose = purpose;
		this.expiresAt = expiresAt;
		this.usedAt = usedAt;
		this.createdAt = createdAt;
	}

	static OneTimeToken create(String tokenHash, UUID userId, TokenPurpose purpose, Instant now, Instant expiresAt) {
		OneTimeToken token = new OneTimeToken(tokenHash, userId, purpose, expiresAt, null, now);
		token.newEntity = true;
		return token;
	}

	@Override
	public String getId() {
		return tokenHash;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	public UUID getUserId() {
		return userId;
	}

	public TokenPurpose getPurpose() {
		return purpose;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
