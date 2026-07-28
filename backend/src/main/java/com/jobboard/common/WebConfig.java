package com.jobboard.common;

import com.jobboard.common.auth.AdminInterceptor;
import com.jobboard.common.auth.AuthInterceptor;
import com.jobboard.user.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;

    public WebConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/health");
        registry.addInterceptor(new AdminInterceptor(userRepository))
                .addPathPatterns("/api/admin/**");
    }
}
