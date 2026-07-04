package com.xd.smartworksite.knowledge.facade;

import java.util.List;

public interface KnowledgeScopeFacade {

    List<Long> validateEnabledKnowledgeBases(Long projectId, List<Long> knowledgeBaseIds);
}
