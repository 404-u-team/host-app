package dev.hostapp.backend.config;

import dev.hostapp.backend.error.ApiError;
import dev.hostapp.backend.security.JwtService;
import dev.hostapp.backend.security.RequestAuthentication;
import dev.hostapp.backend.service.AuthService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Host App API", version = "1.0"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SecurityConfig {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtService jwt, AuthService legacy,
                                    @Value("${app.cors.allowed-origins}") String origins) throws Exception {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
        cors.setExposedHeaders(List.of("X-Correlation-ID"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return http
                .securityMatcher("/api/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                        "/v3/api-docs.yaml", "/actuator/**")
                .cors(config -> config.configurationSource(source))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) ->
                                ApiError.write(response, 401, "UNAUTHORIZED", "Сначала войдите в систему"))
                        .accessDeniedHandler((request, response, ex) ->
                                ApiError.write(response, 403, "FORBIDDEN", "Недостаточно прав")))
                .authorizeHttpRequests(access -> access
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                                "/v3/api-docs.yaml", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/auth/change-password").authenticated()
                        .requestMatchers("/api/references/**", "/api/analytics/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("EDITOR", "ADMIN")
                        .anyRequest().denyAll())
                .addFilterBefore(new RequestAuthentication(jwt, legacy), AnonymousAuthenticationFilter.class)
                .build();
    }
}
