package com.teachdash.accounts.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class User implements Persistable<UUID> {

	@Id
	private final UUID id;
	private final String email;
	private String name;
	private final Role role;
	private String passwordHash;
	private String timeZone;
	private boolean active;
	private final Instant createdAt;

	@Transient
	private boolean newEntity;

	@PersistenceCreator
	User(UUID id, String email, String name, Role role, String passwordHash, String timeZone, boolean active,
			Instant createdAt) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.role = role;
		this.passwordHash = passwordHash;
		this.timeZone = timeZone;
		this.active = active;
		this.createdAt = createdAt;
	}

	public static User create(String email, String name, Role role, String passwordHash, String timeZone,
			Instant now) {
		User user = new User(UUID.randomUUID(), email, name, role, passwordHash, timeZone, true, now);
		user.newEntity = true;
		return user;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void updateProfile(String name, String timeZone) {
		this.name = name;
		this.timeZone = timeZone;
	}
}
