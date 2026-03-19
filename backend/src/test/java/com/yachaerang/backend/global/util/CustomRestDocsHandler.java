package com.yachaerang.backend.global.util;

import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;
import org.springframework.restdocs.snippet.Snippet;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

final class CustomRestDocsHandler {

    private CustomRestDocsHandler() {}

    /**
     * 기본 문서화: prettyPrint + {class-name}/{identifier} 경로 패턴
     */
    static RestDocumentationResultHandler customDocument(
            final String identifier,
            final Snippet... snippets) {

        return document("{class-name}/" + identifier,
                getDocumentRequest(),
                getDocumentResponse(),
                snippets);
    }

    /**
     * 민감 헤더 제거가 필요한 경우
     */
    static RestDocumentationResultHandler customDocument(
            final String identifier,
            final String[] sensitiveRequestHeadersToRemove,
            final String[] sensitiveResponseHeadersToRemove,
            final Snippet... snippets) {

        return document("{class-name}/" + identifier,
                getDocumentRequest(sensitiveRequestHeadersToRemove),
                getDocumentResponse(sensitiveResponseHeadersToRemove),
                snippets);
    }

    private static OperationRequestPreprocessor getDocumentRequest(String... headersToRemove) {
        if (headersToRemove == null || headersToRemove.length == 0) {
            return preprocessRequest(prettyPrint());
        }
        return preprocessRequest(removeHeaders(headersToRemove), prettyPrint());
    }

    private static OperationResponsePreprocessor getDocumentResponse(String... headersToRemove) {
        if (headersToRemove == null || headersToRemove.length == 0) {
            return preprocessResponse(prettyPrint());
        }
        return preprocessResponse(removeHeaders(headersToRemove), prettyPrint());
    }
}
