package com.yachaerang.backend.infrastructure.rag;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.StructuralNode;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AsciidocDocumentReader {

    private final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

    public List<Document> loadDocuments(Resource resource) throws IOException {
        String content = resource.getContentAsString(StandardCharsets.UTF_8);

        org.asciidoctor.ast.Document adocDocument = asciidoctor.load(content, Options.builder().build());

        List<Document> documents = new ArrayList<>();
        extractSections(adocDocument, documents, resource.getFilename());

        return documents;
    }

    private void extractSections(StructuralNode node, List<Document> documents, String source) {
        for (StructuralNode block : node.getBlocks()) {
            if (block.getContext().equals("section")) {
                String sectionTitle = block.getTitle();
                String sectionContent = extractContent(block);

                if (!sectionContent.isBlank()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("source", source);
                    metadata.put("section", sectionTitle != null ? sectionTitle : "");
                    metadata.put("level", block.getLevel());

                    documents.add(new Document(sectionContent, metadata));
                }

                extractSections(block, documents, source);
            }
        }
    }

    private String extractContent(StructuralNode section) {
        StringBuilder sb = new StringBuilder();
        if (section.getTitle() != null) {
            sb.append(section.getTitle()).append("\n\n");
        }
        for (StructuralNode block : section.getBlocks()) {
            if (!block.getContext().equals("section")) {
                sb.append(block.convert()).append("\n");
            }
        }
        return sb.toString().trim();
    }
}