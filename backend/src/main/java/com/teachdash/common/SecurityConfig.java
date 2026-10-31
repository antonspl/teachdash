package com.teachdash.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// STUB: replaced by the real security configuration in pass 2
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	@Bean
	SecurityFilterChain stubFilterChain(HttpSecurity http) throws Exception {
		// STUB: permitAll + CSRF off must never reach main as is
		return http
				.authorizeHttpRequests(a -> a.anyRequest().permitAll())
				.csrf(c -> c.disable())
				.build();
	}
}
