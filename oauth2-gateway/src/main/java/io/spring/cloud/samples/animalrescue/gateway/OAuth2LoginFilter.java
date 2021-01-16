package io.spring.cloud.samples.animalrescue.gateway;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.ServerWebExchange;

public class OAuth2LoginFilter implements GatewayFilter {

	private final SecurityWebFilterChain webFilterChain;

	public OAuth2LoginFilter(SecurityWebFilterChain securityWebFilterChain) {
		this.webFilterChain = securityWebFilterChain;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		WebFilterChainProxy webFilterChainProxy = new WebFilterChainProxy(webFilterChain);
		return webFilterChainProxy.filter(exchange, chain::filter);
	}
}
