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

    @Test
    @DisplayName("GET 요청 성공")
    void testGetSuccess() {
        // given
        String responseBody = "Success";
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        UnaryOperator<UriBuilder> params = b -> b;

        // when
        String result = batchHttpTemplate.get("/test", String.class, params);

        // then
        assertThat(result).isEqualTo(responseBody);
    }

    @Test
    @DisplayName("POST 요청 성공")
    void testPostSuccess() {
        // given
        String responseBody = "Created";
        ClientResponse response = ClientResponse.create(HttpStatus.CREATED)
                .header("Content-Type", "text/plain")
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        UnaryOperator<UriBuilder> params = b -> b;

        // when
        String result = batchHttpTemplate.post("/test", String.class, params);

        // then
        assertThat(result).isEqualTo(responseBody);
    }

    @Test
    @DisplayName("4xx Client Error 발생시 BatchException(BATCH_BAD_REQUEST)")
    void test4xxClientError() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "text/plain")
                .body("Bad Request")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        UnaryOperator<UriBuilder> params = b -> b;

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, params))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_BAD_REQUEST);
    }

    @Test
    @DisplayName("5xx Server Error 발생시 BatchException(BATCH_SERVER_ERROR)")
    void test5xxServerError() {
        // given
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain")
                .body("Server Error")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        UnaryOperator<UriBuilder> params = b -> b;

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, params))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_SERVER_ERROR);
    }

    @Test
    @DisplayName("WebClientException 발생시 BatchException(BATCH_CONNECTION_FAILED)")
    void testWebClientException() {
        // given
        WebClientRequestException wcre = new WebClientRequestException(
                new RuntimeException("Connection Refused"),
                HttpMethod.GET,
                URI.create("/test"),
                org.springframework.http.HttpHeaders.EMPTY
        );
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.error(wcre));

        UnaryOperator<UriBuilder> params = b -> b;

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, params))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_CONNECTION_FAILED);
    }

    @Test
    @DisplayName("Timeout 발생시 BatchException(BATCH_TIMEOUT)")
    void testTimeout() {
        // given
        // Mock WebClient internal timeout operator behavior simulation
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.never());
        
        UnaryOperator<UriBuilder> params = b -> b;

        // when & then
        assertThatThrownBy(() -> batchHttpTemplate.get("/test", String.class, params))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_TIMEOUT);
    }
}
