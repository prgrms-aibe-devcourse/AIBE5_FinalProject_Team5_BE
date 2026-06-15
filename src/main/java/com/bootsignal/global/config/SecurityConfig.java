package com.bootsignal.global.config;

import com.bootsignal.global.config.properties.CorsProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.jwt.JwtAuthenticationFilter;
import com.bootsignal.global.security.jwt.JwtTokenProvider;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * JWT 기반 인증, CORS, 공개/보호 API 경계를 설정하는 보안 구성입니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CorsProperties corsProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/health", "/actuator/health", "/actuator/info", "/h2-console/**").permitAll()
				.requestMatchers("/local-files/profile/**").permitAll()
				.requestMatchers(
					"/api/auth/signup",
					"/api/auth/login",
					"/api/auth/google/login",
					"/api/auth/kakao/login",
					"/api/auth/refresh",
					"/api/auth/logout"
				).permitAll()
				.requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/institutions", "/api/institutions/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/reviews", "/api/reviews/**").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/work24/**").hasRole("ADMIN")
				.anyRequest().authenticated()
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) ->
					handlerExceptionResolver.resolveException(
						request,
						response,
						null,
						new BootSignalException(ErrorCode.UNAUTHORIZED)
					)
				)
				.accessDeniedHandler((request, response, accessDeniedException) ->
					handlerExceptionResolver.resolveException(
						request,
						response,
						null,
						new BootSignalException(ErrorCode.FORBIDDEN)
					)
				)
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
			.build();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
	) {
		return new JwtAuthenticationFilter(jwtTokenProvider, handlerExceptionResolver);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> allowedOrigins = corsProperties.allowedOrigins().stream()
			.filter(Objects::nonNull)
			.filter(StringUtils::hasText)
			.toList();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
