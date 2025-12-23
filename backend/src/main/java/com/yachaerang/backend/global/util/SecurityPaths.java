package com.yachaerang.backend.global.util;

public final class SecurityPaths {

    private SecurityPaths() {}

    /**
     * 인증 없이 접근
     */
    public static final String[] PUBLIC = {
            "/health",
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/mails/**",
            "/api/v1/products/**",
            "/api/v1/articles/**",
            "/api/v1/daily-prices/**",
            "/api/v1/weekly-prices/**",
            "/api/v1/monthly-prices/**",
            "/api/v1/yearly-prices/**"
    };

    /**
     * 로그인 서비스
     */
    public static final String[] USER = {
            "/api/v1/auth/logout",
            "/api/v1/auth/reissue",
            "/api/v1/members/**",
            "/api/v1/favorites/**",
            "/api/v1/chat/**",
            "/api/v1/bookmarks/**",
    };

    /**
     * 관리자 서비스
     */
    public static final String[] ADMIN = {
    };
}
