package com.teachdash.accounts.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;

/** Existing rows are changed only by the targeted updates below, never by save() (design D3). */
public interface UserRepository extends CrudRepository<User, UUID> {

	@Query("select * from users where lower(email) = lower(:email)")
	Optional<User> findByEmailIgnoreCase(@Param("email") String email);

	List<User> findByRoleOrderByName(Role role);

	boolean existsByRole(Role role);

	@Modifying
	@Query("update users set name = :name, time_zone = :timeZone where id = :id")
	void updateProfile(
			@Param("id") UUID id,
			@Param("name") String name,
			@Param("timeZone") String timeZone);

	@Modifying
	@Query("update users set password_hash = :passwordHash where id = :id")
	void updatePasswordHash(
			@Param("id") UUID id,
			@Param("passwordHash") String passwordHash);

	@Modifying
	@Query("update users set active = :active where id = :id")
	void updateActive(
			@Param("id") UUID id,
			@Param("active") boolean active);
}
