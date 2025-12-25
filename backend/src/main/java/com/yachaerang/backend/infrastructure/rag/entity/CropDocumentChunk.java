package com.yachaerang.backend.infrastructure.rag.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CropDocumentChunk {
    private String id;
    private String crop;
    private String ragId;        // CABBAGE-BASE
    private String docType;      // crop-profile
    private String section;      // 재배 기본 정보
    private String content;      // 청크 텍스트
    private List<String> tags;   // [crop, brassica, price, plan, storage]
    private float[] embedding;   // 벡터
}
