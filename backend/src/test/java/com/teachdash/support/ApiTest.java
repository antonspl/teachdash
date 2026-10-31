package com.teachdash.support;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for API tests: real app on a random port, one Postgres
 * Testcontainer shared by all tests, tables emptied before each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTest {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	static {
		POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
	}

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@LocalServerPort
	protected int port;

	@Autowired
	protected JdbcClient jdbc;

	@BeforeEach
	void cleanDatabase() {
		List<String> tables = jdbc.sql("""
				select table_name from information_schema.tables
				where table_schema = 'public' and table_type = 'BASE TABLE'
				  and table_name <> 'flyway_schema_history'
				""").query(String.class).list();
		if (!tables.isEmpty()) {
			jdbc.sql("truncate table " + String.join(", ", tables) + " restart identity cascade").update();
		}
	}
}
