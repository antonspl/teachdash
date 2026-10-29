package com.teachdash.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * STUB (change accounts, task 1.3): lets every request reach the controllers.
 * Replaced by the real security configuration in task 3.2.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	@Bean
	SecurityFilterChain stubFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(a -> a.anyRequest().permitAll())
				.csrf(c -> c.disable())
				.build();
	}
}
