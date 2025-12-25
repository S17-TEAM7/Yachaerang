package com.yachaerang.backend.infrastructure.rag.service;

import com.yachaerang.backend.infrastructure.rag.entity.CropDocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CropDocumentParser {

    private final EmbeddingModel embeddingModel;

    private static final Pattern RAG_ID_PATTERN =
            Pattern.compile("==\\s*\\[RAG-ID:\\s*([A-Z0-9-]+)]\\s*(.+)");
    private static final Pattern METADATA_PATTERN =
            Pattern.compile("^:([a-z-]+):\\s*(.+)$");

    public List<CropDocumentChunk> parse(String content) {
        List<CropDocumentChunk> chunks = new ArrayList<>();

        // 메타데이터 추출
        Map<String, String> metadata = extractMetadata(content);
        String crop = metadata.getOrDefault("crop", "unknown");
        String docType = metadata.getOrDefault("doc-type", "unknown");
        List<String> tags = Arrays.asList(
                metadata.getOrDefault("tags", "").split(",")
        );

        // RAG-ID 섹션별로 분리
        String[] sections = content.split("(?=== \\[RAG-ID:)");

        for (String section : sections) {
            Matcher matcher = RAG_ID_PATTERN.matcher(section);
            if (matcher.find()) {
                String ragId = matcher.group(1);
                String sectionTitle = matcher.group(2).trim();
                String sectionContent = section.substring(matcher.end()).trim();

                // 임베딩 생성
                String textForEmbedding = sectionTitle + "\n" + sectionContent;
                float[] embedding = embeddingModel.embed(textForEmbedding);

                chunks.add(CropDocumentChunk.builder()
                        .id(crop.toLowerCase() + ":" + ragId)
                        .crop(crop)
                        .ragId(ragId)
                        .docType(docType)
                        .section(sectionTitle)
                        .content(sectionContent)
                        .tags(tags)
                        .embedding(embedding)
                        .build());
            }
        }

        return chunks;
    }

    private Map<String, String> extractMetadata(String content) {
        Map<String, String> metadata = new HashMap<>();
        for (String line : content.split("\n")) {
            Matcher matcher = METADATA_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                metadata.put(matcher.group(1), matcher.group(2));
            }
        }
        return metadata;
    }
}
