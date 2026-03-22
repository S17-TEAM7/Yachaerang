package com.yachaerang.backend.infrastructure.batch.client;

import com.yachaerang.backend.global.exception.BatchException;
import com.yachaerang.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.UnaryOperator;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebClientBatchHttpTemplate implements BatchHttpTemplate {

    private final WebClient batchWebClient;

    private static final Duration TIMEOUT = Duration.ofMillis(10000);

    @Override
    public <T> T get(String path, Class<T> type, UnaryOperator<UriBuilder> params) {
        log.info("[BatchHttpTemplate] GET {}", path);
        return execute(
                batchWebClient.get()
                        .uri(b -> params.apply(b.path(path)).build())
                        .retrieve(),
                path, type);
    }

    @Override
    public <T> T post(String path, Class<T> type, UnaryOperator<UriBuilder> params) {
        log.info("[BatchHttpTemplate] POST {}", path);
        return execute(
                batchWebClient.post()
                        .uri(b -> params.apply(b.path(path)).build())
                        .retrieve(),
                path, type);
    }

    private <T> T execute(WebClient.ResponseSpec spec, String uri, Class<T> type) {
        return spec
                .onStatus(status -> status.value() == 404, r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("No Exception Body")
                        .map(body -> BatchException.of(ErrorCode.BATCH_JOB_NOT_FOUND)))
                .onStatus(HttpStatusCode::is4xxClientError, r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("No Exception Body")
                        .map(body -> BatchException.of(ErrorCode.BATCH_BAD_REQUEST)))
                .onStatus(HttpStatusCode::is5xxServerError, r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("No Exception Body")
                        .map(body -> BatchException.of(ErrorCode.BATCH_SERVER_ERROR)))
                .bodyToMono(type)
                .timeout(TIMEOUT, Mono.error(BatchException.of(ErrorCode.BATCH_TIMEOUT)))
                .onErrorMap(org.springframework.web.reactive.function.client.WebClientException.class,
                        ex -> BatchException.of(ErrorCode.BATCH_CONNECTION_FAILED))
                .block();
    }
}
