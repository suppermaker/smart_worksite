package com.xd.smartworksite.knowledge.mapper;

import com.xd.smartworksite.knowledge.domain.KnowledgeDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeDocumentMapper {
    int insert(KnowledgeDocument document);

    KnowledgeDocument selectById(@Param("documentId") Long documentId);

    List<KnowledgeDocument> selectPage(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                       @Param("indexStatus") String indexStatus,
                                       @Param("keyword") String keyword);

    int markIndexQueued(@Param("documentId") Long documentId,
                        @Param("taskId") Long taskId,
                        @Param("updatedBy") Long updatedBy);

    int markIndexing(@Param("documentId") Long documentId,
                     @Param("updatedBy") Long updatedBy);

    int markIndexSuccess(@Param("documentId") Long documentId,
                         @Param("updatedBy") Long updatedBy);

    int markIndexFailed(@Param("documentId") Long documentId,
                        @Param("errorMessage") String errorMessage,
                        @Param("updatedBy") Long updatedBy);

    int softDelete(@Param("documentId") Long documentId,
                   @Param("updatedBy") Long updatedBy);
}
