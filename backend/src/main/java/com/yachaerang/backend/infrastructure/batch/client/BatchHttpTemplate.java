package com.yachaerang.backend.infrastructure.batch.client;

import org.springframework.web.util.UriBuilder;

import java.util.function.UnaryOperator;

/**
 * Batch와 Http 통신을 위한 Template
 */
public interface BatchHttpTemplate {

    <T> T get(String path, Class<T> type, UnaryOperator<UriBuilder> params);

    <T> T post(String path, Class<T> type, UnaryOperator<UriBuilder> params);
}
