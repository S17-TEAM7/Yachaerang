package com.yachaerang.backend.infrastructure.rag.service;

import com.yachaerang.backend.infrastructure.rag.AsciidocDocumentReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final AsciidocDocumentReader documentReader;
    public void ingestDocuments(List<Resource> resources) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                800,   // defaultChunkSize
                350,   // minChunkSizeChars
                200,   // minChunkLengthToEmbed
                100,   // maxNumChunks
                true   // keepSeparator
        );

        for (Resource resource : resources) {
            List<Document> documents = null;
            try {
                documents = documentReader.loadDocuments(resource);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<Document> chunks = splitter.apply(documents);
            vectorStore.add(chunks);
        }
    }
}
