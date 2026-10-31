package com.teachdash.accounts.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;

public interface UserRepository extends CrudRepository<User, UUID> {

	@Query("select * from users where lower(email) = lower(:email)")
	Optional<User> findByEmailIgnoreCase(@Param("email") String email);

	List<User> findByRoleOrderByName(Role role);

	boolean existsByRole(Role role);
}
