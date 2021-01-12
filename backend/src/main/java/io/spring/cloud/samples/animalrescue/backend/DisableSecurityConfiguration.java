package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@Profile("no-auth")
public class DisableSecurityConfiguration {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		// @formatter:off
		return http
			.authorizeExchange().anyExchange().permitAll()
			.and()
			.httpBasic().disable()
			.csrf().disable()
			.build();
		// @formatter:on
	}

}
