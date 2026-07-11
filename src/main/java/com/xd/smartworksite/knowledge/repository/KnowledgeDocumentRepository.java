package com.xd.smartworksite.knowledge.repository;

import com.xd.smartworksite.knowledge.domain.KnowledgeDocument;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository {
    KnowledgeDocument insert(KnowledgeDocument document);

    Optional<KnowledgeDocument> findById(Long documentId);

    List<KnowledgeDocument> findPage(Long knowledgeBaseId, String indexStatus, String keyword);

    int markIndexQueued(Long documentId, Long taskId, Long updatedBy);

    int markIndexing(Long documentId, Long updatedBy);

    int markIndexSuccess(Long documentId, Long updatedBy);

    int markIndexFailed(Long documentId, String errorMessage, Long updatedBy);

    int softDelete(Long documentId, Long updatedBy);
}
