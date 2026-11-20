package dev.hostapp.backend.config;

import dev.hostapp.backend.security.AuthInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class AppConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    public AppConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
            .addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/",
                "/index.html",
                "/assets/**",
                "/auth/register",
                "/auth/verify-email",
                "/auth/resend-email-code",
                "/auth/login",
                "/health",
                "/api/**",
                "/actuator/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/v3/api-docs.yaml"
            );
    }
}
