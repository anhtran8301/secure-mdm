package com.anhtran.securemdm.security;

import com.anhtran.securemdm.exception.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * Stateless bearer authentication. CSRF applies only to the cookie-authenticated auth endpoints;
 * all other APIs require a bearer token and never read cookies.
 */
@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      BearerAuthenticationManager authenticationManager,
      BearerTokenConverter tokenConverter,
      SecurityErrorHandler errors) {
    AuthenticationWebFilter bearer = new AuthenticationWebFilter(authenticationManager);
    bearer.setServerAuthenticationConverter(tokenConverter);
    bearer.setAuthenticationFailureHandler(
        (filter, ex) -> errors.write(filter.getExchange(), ErrorCode.UNAUTHORIZED));
    return http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    .accessDeniedHandler(errors)
                    .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher(
                        ServerWebExchangeMatchers.pathMatchers(
                            HttpMethod.POST,
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout")))
        .exceptionHandling(
            handling -> handling.authenticationEntryPoint(errors).accessDeniedHandler(errors))
        .authorizeExchange(
            rules ->
                rules
                    .pathMatchers(
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/device-enrollments",
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**")
                    .permitAll()
                    .pathMatchers("/api/v1/device-agent/**")
                    .hasRole("DEVICE")
                    .pathMatchers("/api/v1/**")
                    .hasAnyRole("ORG_ADMIN", "OPERATOR", "VIEWER")
                    .anyExchange()
                    .denyAll())
        .addFilterAt(bearer, SecurityWebFiltersOrder.AUTHENTICATION)
        .build();
  }
}
