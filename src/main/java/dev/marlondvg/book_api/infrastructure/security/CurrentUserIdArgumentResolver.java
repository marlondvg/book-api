package dev.marlondvg.book_api.infrastructure.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUserId.class)
				&& UUID.class.equals(parameter.getParameterType());
	}

	@Override
	public UUID resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new AuthenticationCredentialsNotFoundException("No authenticated user");
		}
		try {
			return UUID.fromString(jwtAuthentication.getToken().getSubject());
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new AuthenticationCredentialsNotFoundException("Token subject is not a user id");
		}
	}
}
