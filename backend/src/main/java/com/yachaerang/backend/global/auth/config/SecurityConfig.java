package com.yachaerang.backend.global.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yachaerang.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.yachaerang.backend.global.auth.jwt.JwtTokenProvider;
import com.yachaerang.backend.global.response.ErrorCode;
import com.yachaerang.backend.global.response.ErrorResponse;
import com.yachaerang.backend.global.util.SecurityPaths;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
Spring Security 관련 설정 Configuration
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /*
    암호화 등록
     */
    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
    Security에 대한 필터 등록
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(httpSecurityCorsConfigurer -> {
                    httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());
                })
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagerConfigurer -> {
                    sessionManagerConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                // 비동기 요청에서도 SecurityContext 유지
                .securityContext(securityContext ->
                        securityContext.requireExplicitSave(false))
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(unauthorizedEntryPoint());
                    exception.accessDeniedHandler(accessDeniedHandler());
                })
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // Async Dispatcher
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/articles/reactions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/articles/reactions").authenticated()
                        .requestMatchers(SecurityPaths.PUBLIC).permitAll()
                        .requestMatchers(SecurityPaths.ADMIN).hasRole("ADMIN")
                        .requestMatchers(SecurityPaths.USER).hasAnyRole("USER", "ADMIN")
                        // 인증 필요
                        .anyRequest().authenticated()
                );

        setJwtTokenProvider(httpSecurity);
        return  httpSecurity.build();
    }

    /*
    설정한 JWT Filter를 기반으로 Security Filter에 적용
     */
    private void setJwtTokenProvider(HttpSecurity httpSecurity) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /*
    필터 차원에서의 예외처리를 위한 EntryPoint
    필터 체인에서 예외 금지
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            ErrorCode errorCode = ErrorCode.UNAUTHORIZED_ACCESS;
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .httpStatus(errorCode.getHttpStatus())
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .build();

            response.setStatus(errorCode.getHttpStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));        };
    }

    /*
    접근 권한 예외
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .httpStatus(errorCode.getHttpStatus())
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .build();

            response.setStatus(errorCode.getHttpStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }

    /*
    CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5173"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
