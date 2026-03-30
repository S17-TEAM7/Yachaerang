package com.yachaerang.backend.global.filter;

import com.yachaerang.backend.global.util.LogUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String MDC_REQUEST_ID = "requestID";

    public LoggingFilter() {
        super();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        if (request.getRequestURI().startsWith("/api/v1/farms") ||
        request.getRequestURI().startsWith("/api/v1/mails") ) {
            return true;
        }
        return super.shouldNotFilter(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 요청 마다의 requestId 생성
        String requestId = Optional.ofNullable(request.getHeader(HEADER_REQUEST_ID))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        long start = System.currentTimeMillis();

        // MDC
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            // Log
            logRequest(requestWrapper, requestId);
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception ex) {
            // Error
            LogUtil.error(ex, requestWrapper);
        } finally {
            logResponse(responseWrapper, start, requestId);
            // response 바디를 실제로 내보내기
            responseWrapper.copyBodyToResponse();
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    /*
     Request 기록
     */
    private void logRequest(HttpServletRequest request, String requestId) {

        if (request.getRequestURI().startsWith("/actuator/prometheus")) {
            return;
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        LogUtil.info(
                "[REQ] id={} {} {}{}",
                requestId, method, uri, (query != null ? query : "")
        );
    }

    /*
    Response 콘솔 로그
     */
    private void logResponse(ContentCachingResponseWrapper responseWrapper, long start, String requestId) throws IOException {

        long tookMs = System.currentTimeMillis() - start;
        int status = responseWrapper.getStatus(); // HTTP STATUS

        byte[] body = responseWrapper.getContentAsByteArray();
        StringBuilder bodyStringBuilder = new StringBuilder();
        String bodyString = "";
        if ( body != null && body.length > 0) {
            bodyString = bodyStringBuilder.append(new String(body, StandardCharsets.UTF_8)).toString();
            if (bodyString.length() > 2000) {
                bodyString = bodyString.substring(0, 2000) + "...(omission)";
            }
        }
        LogUtil.info("[RES] id={} status={} took={}", requestId, status, tookMs);
        LogUtil.debug("[RES-BODY] {}", bodyString);

        // response 바디를 실제로 내보내기
        responseWrapper.copyBodyToResponse();
    }
}
