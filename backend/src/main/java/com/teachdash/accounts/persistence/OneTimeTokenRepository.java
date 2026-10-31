package com.teachdash.accounts.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.teachdash.accounts.domain.OneTimeToken;

public interface OneTimeTokenRepository extends CrudRepository<OneTimeToken, String> {

	@Modifying
	@Query("update one_time_tokens set used_at = :now where user_id = :userId and used_at is null")
	int markUnusedAsUsed(@Param("userId") UUID userId, @Param("now") Instant now);

	/** Returns 1 only for the caller that wins the unused, unexpired token. */
	@Modifying
	@Query("update one_time_tokens set used_at = :now where token_hash = :hash and used_at is null and expires_at > :now")
	int markUsed(@Param("hash") String hash, @Param("now") Instant now);
}
