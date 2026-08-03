package com.learning.cart_service.config;

import com.learning.cart_service.security.CurrentUserIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers){
        resolvers.add(new CurrentUserIdArgumentResolver());
    }
}
