package dev.marlondvg.book_api.infrastructure.security;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
class CurrentUserWebConfig implements WebMvcConfigurer {

	static {
		// The current user comes from the token, so it must not appear as an API parameter.
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId.class);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentUserIdArgumentResolver());
	}
}
