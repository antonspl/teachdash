package com.teachdash.languages.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.languages.domain.Language;

@Repository
public class LanguageRepository {

	private final JdbcClient jdbc;

	LanguageRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<Language> findAll() {
		return jdbc.sql("select code, content from languages order by code")
				.query((rs, i) -> new Language(rs.getString("code"), rs.getBoolean("content")))
				.list();
	}
}
