package com.teachdash.languages.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.languages.api.dto.LanguageResponse;

@Repository
public class LanguageRepository {

	private final JdbcClient jdbc;

	LanguageRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<LanguageResponse> findAll() {
		return jdbc.sql("select code, content from languages order by code")
				.query((rs, i) -> new LanguageResponse(rs.getString("code"), rs.getBoolean("content")))
				.list();
	}

	public boolean exists(String code) {
		return jdbc.sql("select exists (select 1 from languages where code = :code)")
				.param("code", code)
				.query(Boolean.class)
				.single();
	}
}
