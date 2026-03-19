package com.yachaerang.backend.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper;

    protected abstract Object initController();

    protected Object[] initControllerAdvices() {
        return new Object[0];
    }

    protected Filter[] initFilters() {
        return new Filter[0];
    }

    protected boolean isPrintEnabled() {
        return true;
    }

    @BeforeEach
    protected void setUp(RestDocumentationContextProvider provider) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        StandaloneMockMvcBuilder builder = MockMvcBuilders
                .standaloneSetup(initController())
                .setMessageConverters(jacksonConverter)
                .addFilters(new CharacterEncodingFilter("UTF-8", true));

        if (isPrintEnabled()) {
            builder.alwaysDo(print());
        }

        Object[] advices = initControllerAdvices();
        if (advices.length > 0) {
            builder.setControllerAdvice(advices);
        }

        Filter[] filters = initFilters();
        if (filters.length > 0) {
            builder.addFilters(filters);
        }

        mockMvc = builder
                .apply(documentationConfiguration(provider))
                .build();
    }

    /**
     * CustomRestDocsHandler 호출
     */
    protected RestDocumentationResultHandler doc(String identifier, Snippet... snippets) {
        return CustomRestDocsHandler.customDocument(identifier, snippets);
    }

    /**
     * CustomRestDocsHandler 호출( 민감 헤더 제거용 )
     */
    protected RestDocumentationResultHandler doc(
            String identifier,
            String[] sensitiveRequestHeadersToRemove,
            String[] sensitiveResponseHeadersToRemove,
            Snippet... snippets
    ) {
        return CustomRestDocsHandler.customDocument(
                identifier,
                sensitiveRequestHeadersToRemove,
                sensitiveResponseHeadersToRemove,
                snippets
        );
    }
}
