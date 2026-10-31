package com.teachdash.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// STUB: replaced by the real security configuration in pass 2
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	// STUB: an empty user store stops Spring Boot generating and logging a password
	@Bean
	UserDetailsService stubUserDetailsService() {
		return new InMemoryUserDetailsManager();
	}

	@Bean
	SecurityFilterChain stubFilterChain(HttpSecurity http) throws Exception {
		// STUB: permitAll + CSRF off must never reach main as is
		return http
				.authorizeHttpRequests(a -> a.anyRequest().permitAll())
				.csrf(c -> c.disable())
				.build();
	}
}
