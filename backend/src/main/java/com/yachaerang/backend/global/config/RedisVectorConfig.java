package com.yachaerang.backend.global.config;

import com.yachaerang.backend.global.util.LogUtil;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

import java.util.Map;

@Configuration
public class RedisVectorConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    @Value("${app.vector.dimension:1536}")
    private int vectorDimension;

    @Bean
    public JedisPooled jedisPool() {
        return new JedisPooled(
                new HostAndPort(redisHost, redisPort),
                DefaultJedisClientConfig.builder()
                        .password(redisPassword)
                        .build()
        );
    }

    @Bean
    public CommandLineRunner initVectorIndex(JedisPooled jedisPooled) {
        return args -> {
            createCropVectorIndex(jedisPooled);
        };
    }

    private void createCropVectorIndex(JedisPooled jedisPooled) {
        String indexName = "idx:crop-documents";
        try {
            jedisPooled.ftInfo(indexName);
            LogUtil.info("Vector index already exists: {}", indexName);
        } catch (JedisDataException e) {
            // 인덱스가 없으면 생성
            Schema schema = new Schema()
                    // 메타데이터 필드
                    .addTextField("crop", 2.0)           // 작물명
                    .addTextField("ragId", 1.5)          // RAG-ID
                    .addTextField("docType", 1.0)        // 문서 타입
                    .addTextField("content", 1.0)        // 청크 내용
                    .addTagField("tags")                 // 태그
                    .addTextField("section", 1.0)        // 섹션명

                    .addVectorField("embedding",
                            Schema.VectorField.VectorAlgo.HNSW,
                            Map.of(
                                    "TYPE", "FLOAT32",
                                    "DIM", vectorDimension,
                                    "DISTANCE_METRIC", "COSINE",
                                    "M", 16,
                                    "EF_CONSTRUCTION", 200
                            ));

            IndexDefinition definition = new IndexDefinition()
                    .setPrefixes("crop:");

            jedisPooled.ftCreate(indexName, IndexOptions.defaultOptions()
                    .setDefinition(definition), schema);

            LogUtil.info("Created vector index: {}", indexName);
        }
    }

    @Bean
    public VectorStore redisVectorStore(
            OpenAiApi.EmbeddingModel embeddingModel,
            JedisPooled jedisPooled) {

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("crop-rag-index")
                .prefix("crop:")
                .initializeSchema(true)
                .build();
    }
}
