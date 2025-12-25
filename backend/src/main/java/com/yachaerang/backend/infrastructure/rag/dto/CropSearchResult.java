package com.yachaerang.backend.infrastructure.rag.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropSearchResult {
    private String id;
    private String crop;
    private String ragId;
    private String section;     // 재배 기본 정보
    private String content;     // 청크 텍스트 내용
    private List<String> tags;  // [crop, brassica, price, ...]
    private Double score;       // 유사도 점수(낮을수록 유사)

    /**
     * 유사도 퍼센트 변환 (COSINE 거리 기준)
     * COSINE 거리: 0 = 완전 일치, 2 = 완전 반대
     */
    public double getSimilarityPercent() {
        return (1 - score / 2) * 100;
    }
}
