package com.yachaerang.backend.infrastructure.batch.client;

import com.yachaerang.backend.global.exception.BatchException;
import com.yachaerang.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebClientBatchHttpTemplateTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private WebClientBatchHttpTemplate batchHttpTemplate;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        batchHttpTemplate = new WebClientBatchHttpTemplate(webClient);
    }

    // ─────────────────────────────────────────
    // GET 성공
    // ─────────────────────────────────────────

    @Test
    @DisplayName("GET 요청 성공 시 응답 본문을 반환한다")
    void testGetSuccess() {
        // given
        String responseBody = "Success";
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when
        String result = batchHttpTemplate.get("/test", String.class, b -> b);

        // then
        assertThat(result).isEqualTo(responseBody);
    }

    // ─────────────────────────────────────────
    // POST 성공
    // ─────────────────────────────────────────

    @Test
    @DisplayName("POST 요청 성공 시 응답 본문을 반환한다")
    void testPostSuccess() {
        // given
        String responseBody = "Created";
        ClientResponse response = ClientResponse.create(HttpStatus.CREATED)
                .header("Content-Type", "text/plain")
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when
        String result = batchHttpTemplate.post("/test", String.class, b -> b);

        // then
        assertThat(result).isEqualTo(responseBody);
    }

    // ─────────────────────────────────────────
    // 404 처리 (BATCH_JOB_NOT_FOUND)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("404 응답 시 BatchException(BATCH_JOB_NOT_FOUND) 발생")
    void test404NotFound() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.NOT_FOUND)
                .header("Content-Type", "text/plain")
                .body("Not Found")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("404 응답 본문이 없을 때도 BatchException(BATCH_JOB_NOT_FOUND) 발생 (defaultIfEmpty 분기)")
    void test404NotFoundEmptyBody() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.NOT_FOUND)
                .header("Content-Type", "text/plain")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_JOB_NOT_FOUND);
    }

    // ─────────────────────────────────────────
    // 4xx 처리 (BATCH_BAD_REQUEST)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("4xx 응답 시 BatchException(BATCH_BAD_REQUEST) 발생")
    void test4xxClientError() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "text/plain")
                .body("Bad Request")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_BAD_REQUEST);
    }

    @Test
    @DisplayName("4xx 응답 본문이 없을 때도 BatchException(BATCH_BAD_REQUEST) 발생 (defaultIfEmpty 분기)")
    void test4xxClientErrorEmptyBody() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.UNPROCESSABLE_ENTITY)
                .header("Content-Type", "text/plain")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_BAD_REQUEST);
    }

    @Test
    @DisplayName("POST 4xx 응답 시 BatchException(BATCH_BAD_REQUEST) 발생")
    void testPost4xxClientError() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "text/plain")
                .body("Bad Request")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.post("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_BAD_REQUEST);
    }

    // ─────────────────────────────────────────
    // 5xx 처리 (BATCH_SERVER_ERROR)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("5xx 응답 시 BatchException(BATCH_SERVER_ERROR) 발생")
    void test5xxServerError() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain")
                .body("Server Error")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_SERVER_ERROR);
    }

    @Test
    @DisplayName("5xx 응답 본문이 없을 때도 BatchException(BATCH_SERVER_ERROR) 발생 (defaultIfEmpty 분기)")
    void test5xxServerErrorEmptyBody() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Content-Type", "text/plain")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_SERVER_ERROR);
    }

    @Test
    @DisplayName("POST 5xx 응답 시 BatchException(BATCH_SERVER_ERROR) 발생")
    void testPost5xxServerError() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain")
                .body("Server Error")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.post("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_SERVER_ERROR);
    }

    // ─────────────────────────────────────────
    // 연결 실패 처리 (BATCH_CONNECTION_FAILED)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("GET WebClientException 발생 시 BatchException(BATCH_CONNECTION_FAILED) 발생")
    void testGetWebClientException() {
        // given
        WebClientRequestException wcre = new WebClientRequestException(
                new RuntimeException("Connection Refused"),
                HttpMethod.GET,
                URI.create("/test"),
                org.springframework.http.HttpHeaders.EMPTY
        );
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.error(wcre));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_CONNECTION_FAILED);
    }

    @Test
    @DisplayName("POST WebClientException 발생 시 BatchException(BATCH_CONNECTION_FAILED) 발생")
    void testPostWebClientException() {
        // given
        WebClientRequestException wcre = new WebClientRequestException(
                new RuntimeException("Connection Refused"),
                HttpMethod.POST,
                URI.create("/test"),
                org.springframework.http.HttpHeaders.EMPTY
        );
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.error(wcre));

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.post("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_CONNECTION_FAILED);
    }

    // ─────────────────────────────────────────
    // 타임아웃 처리 (BATCH_TIMEOUT)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("GET Timeout 발생 시 BatchException(BATCH_TIMEOUT) 발생")
    void testGetTimeout() {
        // given
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.never());

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_TIMEOUT);
    }

    @Test
    @DisplayName("POST Timeout 발생 시 BatchException(BATCH_TIMEOUT) 발생")
    void testPostTimeout() {
        // given
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.never());

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.post("/test", String.class, b -> b))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_TIMEOUT);
    }
}
