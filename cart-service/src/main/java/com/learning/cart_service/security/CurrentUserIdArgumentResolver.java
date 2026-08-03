package com.learning.cart_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * HandlerMethodArgumentResolver is the SAME extension point Spring MVC
 * uses internally for things like @RequestParam, @PathVariable, and
 * @RequestBody — we're plugging into the exact mechanism the framework
 * itself is built on, not working around it. Once registered (Step 2),
 * ANY controller method anywhere in this service can just declare a
 * parameter as `@CurrentUserId Long userId` and Spring resolves it
 * automatically before the method runs — same ergonomics as a built-in
 * annotation.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String userIdHeader = request.getHeader("X-Auth-User-Id");

        if(userIdHeader == null){
            // Only reachable if this service is called directly,
            // bypassing the gateway — a real gap that Phase D File 2's
            // network-isolation assumption is meant to close in an
            // actual deployment. Surfacing a clear error here (rather
            // than a confusing NullPointerException downstream) makes
            // that assumption's importance concrete rather than abstract.
            throw new IllegalStateException(
                    "Missing X-Auth-User-Id header - this endpoint must be called through the API Gateway");
        }

        return Long.valueOf(userIdHeader);
    }
}
