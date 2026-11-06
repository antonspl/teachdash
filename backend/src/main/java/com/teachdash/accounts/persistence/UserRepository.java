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

	@Query("select * from users where id = :id for update")
	Optional<User> findByIdForUpdate(@Param("id") UUID id);

	@Query("select * from users where id = :id and active for update")
	Optional<User> findActiveByIdForUpdate(@Param("id") UUID id);

	List<User> findByRoleOrderByName(Role role);

	boolean existsByRole(Role role);

	boolean existsByIdAndRole(UUID id, Role role);

	@Query("update users set name = :name, time_zone = :timeZone where id = :id returning *")
	Optional<User> updateProfile(
			@Param("id") UUID id,
			@Param("name") String name,
			@Param("timeZone") String timeZone);

	/** Sets the password and raises the session version, so every older session is rejected. */
	@Query("""
			update users set password_hash = :passwordHash, session_version = session_version + 1
			where id = :id
			returning session_version
			""")
	int updatePasswordHashAndRevokeSessions(
			@Param("id") UUID id,
			@Param("passwordHash") String passwordHash);

	/** Empty when the account is inactive or its session version moved on, so nothing was changed. */
	@Query("""
			update users set password_hash = :passwordHash, session_version = session_version + 1
			where id = :id and active and session_version = :expectedSessionVersion
			returning session_version
			""")
	Optional<Integer> updatePasswordHashIfSessionCurrent(
			@Param("id") UUID id,
			@Param("passwordHash") String passwordHash,
			@Param("expectedSessionVersion") int expectedSessionVersion);

	@Modifying
	@Query("update users set active = false, session_version = session_version + 1 where id = :id")
	void deactivate(@Param("id") UUID id);

	@Modifying
	@Query("update users set active = true where id = :id")
	void reactivate(@Param("id") UUID id);
}
