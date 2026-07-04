package com.xd.smartworksite.knowledge.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.knowledge.domain.KnowledgeBase;
import com.xd.smartworksite.knowledge.facade.KnowledgeScopeFacade;
import com.xd.smartworksite.knowledge.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeScopeApplicationService implements KnowledgeScopeFacade {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeScopeApplicationService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Override
    public List<Long> validateEnabledKnowledgeBases(Long projectId, List<Long> knowledgeBaseIds) {
        List<Long> requestedIds = normalizeIds(knowledgeBaseIds);
        if (requestedIds.isEmpty()) {
            return List.of();
        }
        List<KnowledgeBase> knowledgeBases = knowledgeBaseRepository.findByProjectAndIds(projectId, requestedIds);
        Set<Long> foundIds = knowledgeBases.stream()
                .map(KnowledgeBase::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long requestedId : requestedIds) {
            if (!foundIds.contains(requestedId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Knowledge base does not belong to project");
            }
        }
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            if (!knowledgeBase.isEnabled()) {
                throw new BusinessException(ErrorCode.CONFLICT, "Knowledge base is disabled");
            }
        }
        return requestedIds;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        for (Long id : ids) {
            if (id == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Knowledge base id must not be null");
            }
        }
        return ids.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
