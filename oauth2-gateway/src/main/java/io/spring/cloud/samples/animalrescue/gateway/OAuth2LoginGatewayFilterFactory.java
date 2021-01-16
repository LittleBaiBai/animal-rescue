package io.spring.cloud.samples.animalrescue.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

	private final ReactiveJwtDecoder jwtDecoder;
	private final ReactiveClientRegistrationRepository clientRegistrationRepository;

	public OAuth2LoginGatewayFilterFactory(ReactiveJwtDecoder jwtDecoder, ReactiveClientRegistrationRepository clientRegistrationRepository) {
		this.jwtDecoder = jwtDecoder;
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public GatewayFilter apply(Object config) {
		// @formatter:off
		SecurityWebFilterChain chain = ServerHttpSecurity.http()
		                                                 .csrf().disable()
		                                                 .oauth2Login()
		                                                    .clientRegistrationRepository(clientRegistrationRepository).and()
		                                                 .oauth2ResourceServer().jwt().jwtDecoder(jwtDecoder).and()
		                                                 .and()
		                                                    .authorizeExchange()
		                                                    .anyExchange().authenticated()
		                                                 .and()
		                                                 .build();
		// @formatter:on

		return new OAuth2LoginFilter(chain);
	}
}
