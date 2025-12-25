package com.yachaerang.backend.infrastructure.rag.service;

import com.yachaerang.backend.global.util.LogUtil;
import com.yachaerang.backend.infrastructure.rag.dto.CropSearchResult;
import com.yachaerang.backend.infrastructure.rag.entity.CropDocumentChunk;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.search.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CropVectorStoreService {

    private final JedisPooled jedis;
    private final EmbeddingModel embeddingModel;

    private static final String INDEX_NAME = "idx:crop-documents";
    private static final String KEY_PREFIX = "crop:";

    /**
     * 문서 청크 저장
     */
    public void saveChunk(CropDocumentChunk chunk) {
        String key = KEY_PREFIX + chunk.getCrop() + ":" + chunk.getRagId();

        float[] vector = chunk.getEmbedding();
        if (vector == null || vector.length == 0) {
            vector = embeddingModel.embed(chunk.getContent());
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("crop", chunk.getCrop());
        fields.put("ragId", chunk.getRagId());
        fields.put("docType", chunk.getDocType());
        fields.put("section", chunk.getSection());
        fields.put("content", chunk.getContent());
        fields.put("tags", String.join(",", chunk.getTags()));

        fields.put("embedding", floatArrayToBytes(vector));

        jedis.hsetObject(key, fields);
        LogUtil.debug("Saved chunk: {}", key);
    }

    /**
     * 유사 문서 검색
     */
    public List<CropSearchResult> searchSimilar(String query, int topK) {

        float[] queryVector = embeddingModel.embed(query);
        return searchByVector(queryVector, topK, null);
    }

    /**
     * 특정 작물 내에서 검색
     */
    public List<CropSearchResult> searchByCrop(String query, String cropName, int topK) {
        float[] queryVector = embeddingModel.embed(query);
        String filter = "@crop:" + cropName;
        return searchByVector(queryVector, topK, filter);
    }

    /**
     * 태그 기반 필터링 검색
     */
    public List<CropSearchResult> searchByTags(String query, List<String> tags, int topK) {
        float[] queryVector = embeddingModel.embed(query);
        String tagFilter = "@tags:{" + String.join("|", tags) + "}";
        return searchByVector(queryVector, topK, tagFilter);
    }

    private List<CropSearchResult> searchByVector(float[] vector, int topK, String filter) {
        byte[] vectorBytes = floatArrayToBytes(vector);

        String queryStr = filter != null
                ? "(" + filter + ")=>[KNN $K @embedding $BLOB AS score]"
                : "*=>[KNN $K @embedding $BLOB AS score]";

        Query query = new Query(queryStr)
                .addParam("K", topK)
                .addParam("BLOB", vectorBytes)
                .returnFields("crop", "ragId", "section", "content", "tags", "score")
                .setSortBy("score", true)
                .dialect(2);

        try {
            SearchResult result = jedis.ftSearch(INDEX_NAME, query);
            return result.getDocuments().stream()
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LogUtil.error("Redis search failed", e);
            return Collections.emptyList();
        }
    }

    private CropSearchResult toSearchResult(Document doc) {
        // null safe 처리 추가 권장
        return CropSearchResult.builder()
                .id(doc.getId())
                .crop(getStringSafe(doc, "crop"))
                .ragId(getStringSafe(doc, "ragId"))
                .section(getStringSafe(doc, "section"))
                .content(getStringSafe(doc, "content"))
                .tags(Arrays.asList(getStringSafe(doc, "tags").split(",")))
                // Redis Score는 거리(Distance)이므로 유사도로 보고 싶으면 변환 필요할 수 있음
                .score(getDoubleSafe(doc, "score"))
                .build();
    }

    private String getStringSafe(Document doc, String key) {
        Object val = doc.get(key);
        return val != null ? val.toString() : "";
    }

    private double getDoubleSafe(Document doc, String key) {
        Object val = doc.get(key);
        try {
            return val != null ? Double.parseDouble(val.toString()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
}
