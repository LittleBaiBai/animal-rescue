package io.spring.cloud.samples.animalrescue.gateway;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;

@EnableSpringWebSession
@SpringBootApplication
public class GatewayDemoOauthApplication {

	@Bean
	ReactiveMapSessionRepository reactiveSessionRepository() {
		return new ReactiveMapSessionRepository(new ConcurrentHashMap<>());
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
			.csrf().disable()
			.httpBasic().disable()
			.oauth2Login().and()
			.authorizeExchange()
				.pathMatchers("/whoami").authenticated()
				.anyExchange().permitAll()
			.and()
			.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayDemoOauthApplication.class, args);
	}

}
