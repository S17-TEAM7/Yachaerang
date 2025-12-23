package com.yachaerang.backend.infrastructure.rag.service;

import com.yachaerang.backend.infrastructure.rag.entity.QaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryService {
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    @Transactional
    public QaResponse processAndSaveResponse(String question) {
        log.info("Processing question: {}", question);

        // 1. 관련 문서 검색
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .similarityThreshold(0.7)
                        .build()
        );

        log.info("Found {} relevant documents", relevantDocs.size());

        // 2. 컨텍스트 구성
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system("""
                        너는 문서 기반 질의응답 시스템이야.
                        제공된 컨텍스트를 바탕으로 답변을 해.
                        컨텍스트에도 없고, 검색에서도 참조할 수 없다면 '알 수 없습니다.'라고 답변해.                        
                        답변은 명확하고 간결하게 작성하세요.
                        """)
                .user("""
                        컨텍스트:
                        %s
                        
                        질문: %s
                        """.formatted(context, question))
                .call()
                .content();

        String sources = relevantDocs.stream()
                .map(doc -> doc.getMetadata().getOrDefault("source", "unknown").toString())
                .distinct()
                .collect(Collectors.joining(", "));

        return QaResponse.builder()
                .question(question)
                .answer(response)
                .sources(sources)
                .relevantChunks(relevantDocs.size())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
