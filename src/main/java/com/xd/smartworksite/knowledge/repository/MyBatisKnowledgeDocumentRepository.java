package com.xd.smartworksite.knowledge.repository;

import com.xd.smartworksite.knowledge.domain.KnowledgeDocument;
import com.xd.smartworksite.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisKnowledgeDocumentRepository implements KnowledgeDocumentRepository {
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public MyBatisKnowledgeDocumentRepository(KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    @Override
    public KnowledgeDocument insert(KnowledgeDocument document) {
        knowledgeDocumentMapper.insert(document);
        return document;
    }

    @Override
    public Optional<KnowledgeDocument> findById(Long documentId) {
        return Optional.ofNullable(knowledgeDocumentMapper.selectById(documentId));
    }

    @Override
    public List<KnowledgeDocument> findPage(Long knowledgeBaseId, String indexStatus, String keyword) {
        return knowledgeDocumentMapper.selectPage(knowledgeBaseId, indexStatus, keyword);
    }

    @Override
    public int markIndexQueued(Long documentId, Long taskId, Long updatedBy) {
        return knowledgeDocumentMapper.markIndexQueued(documentId, taskId, updatedBy);
    }

    @Override
    public int markIndexing(Long documentId, Long updatedBy) {
        return knowledgeDocumentMapper.markIndexing(documentId, updatedBy);
    }

    @Override
    public int markIndexSuccess(Long documentId, Long updatedBy) {
        return knowledgeDocumentMapper.markIndexSuccess(documentId, updatedBy);
    }

    @Override
    public int markIndexFailed(Long documentId, String errorMessage, Long updatedBy) {
        return knowledgeDocumentMapper.markIndexFailed(documentId, errorMessage, updatedBy);
    }

    @Override
    public int softDelete(Long documentId, Long updatedBy) {
        return knowledgeDocumentMapper.softDelete(documentId, updatedBy);
    }
}
