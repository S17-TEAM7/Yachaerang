package com.yachaerang.backend.infrastructure.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaResponse {

    private Long id;

    private String question;
    private String answer;
    private String sources;

    private Integer relevantChunks;
    private LocalDateTime createdAt;
}