package com.xd.smartworksite.knowledge.infra;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.knowledge.dto.KnowledgeSearchRequest;
import com.xd.smartworksite.knowledge.dto.KnowledgeSearchResponse;
import com.xd.smartworksite.knowledge.dto.KnowledgeSnippetResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.retrieval.http", name = "enabled", havingValue = "true")
public class HttpKnowledgeRetrievalClient implements KnowledgeRetrievalClient {

    private final KnowledgeRetrievalProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final KnowledgeRetrievalRequestFactoryProvider requestFactoryProvider;

    public HttpKnowledgeRetrievalClient(KnowledgeRetrievalProperties properties,
                                        RestClient.Builder restClientBuilder,
                                        KnowledgeRetrievalRequestFactoryProvider requestFactoryProvider) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
        this.requestFactoryProvider = requestFactoryProvider;
    }

    @Override
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request, List<Long> validatedKnowledgeBaseIds) {
        validateConfigured();
        long start = System.nanoTime();
        try {
            Map<?, ?> rawResponse = restClientBuilder.clone()
                    .requestFactory(requestFactoryProvider.requestFactory(Duration.ofMillis(properties.getTimeoutMs())))
                    .build()
                    .post()
                    .uri(properties.getEndpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload(request, validatedKnowledgeBaseIds))
                    .retrieve()
                    .body(Map.class);
            return toResponse(rawResponse, elapsedMs(start));
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Knowledge retrieval HTTP call failed");
        }
    }

    private void validateConfigured() {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Knowledge retrieval endpoint is not configured");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Knowledge retrieval API key is not configured");
        }
        if (properties.getTimeoutMs() == null || properties.getTimeoutMs() < 100 || properties.getTimeoutMs() > 60000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "Knowledge retrieval timeoutMs must be between 100 and 60000");
        }
    }

    private Map<String, Object> payload(KnowledgeSearchRequest request, List<Long> validatedKnowledgeBaseIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", request.getProjectId());
        payload.put("query", request.getQuery());
        payload.put("knowledgeBaseIds", validatedKnowledgeBaseIds);
        payload.put("topK", request.getTopK());
        payload.put("minScore", request.getMinScore());
        payload.put("domain", request.getDomain());
        return payload;
    }

    private KnowledgeSearchResponse toResponse(Map<?, ?> rawResponse, long costMs) {
        if (rawResponse == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Knowledge retrieval returned empty response");
        }
        KnowledgeSearchResponse response = new KnowledgeSearchResponse();
        response.setSnippets(extractSnippets(rawResponse.get("snippets")));
        response.setResultSummary(requiredString(rawResponse, "resultSummary",
                "Knowledge retrieval resultSummary is invalid"));
        response.setCostMs(costMs);
        return response;
    }

    private List<KnowledgeSnippetResponse> extractSnippets(Object snippetsValue) {
        if (!(snippetsValue instanceof List<?> rawSnippets)) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Knowledge retrieval snippets are invalid");
        }
        List<KnowledgeSnippetResponse> snippets = new ArrayList<>();
        for (Object rawSnippet : rawSnippets) {
            if (!(rawSnippet instanceof Map<?, ?> snippetMap)) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "Knowledge retrieval snippet is invalid");
            }
            KnowledgeSnippetResponse snippet = new KnowledgeSnippetResponse();
            snippet.setKnowledgeBaseId(requiredLong(snippetMap, "knowledgeBaseId",
                    "Knowledge retrieval snippet knowledgeBaseId is invalid"));
            snippet.setDocumentId(requiredLong(snippetMap, "documentId",
                    "Knowledge retrieval snippet documentId is invalid"));
            snippet.setTitle(requiredString(snippetMap, "title",
                    "Knowledge retrieval snippet title is invalid"));
            snippet.setSourceType(requiredString(snippetMap, "sourceType",
                    "Knowledge retrieval snippet sourceType is invalid"));
            snippet.setLocation(requiredString(snippetMap, "location",
                    "Knowledge retrieval snippet location is invalid"));
            snippet.setContentExcerpt(requiredString(snippetMap, "contentExcerpt",
                    "Knowledge retrieval snippet contentExcerpt is invalid"));
            snippet.setScore(requiredDouble(snippetMap, "score",
                    "Knowledge retrieval snippet score is invalid"));
            snippet.setRerankScore(optionalDouble(snippetMap, "rerankScore"));
            snippets.add(snippet);
        }
        return snippets;
    }

    private Long requiredLong(Map<?, ?> values, String field, String errorMessage) {
        Object value = values.get(field);
        if (!(value instanceof Number number) || number.longValue() <= 0 || number.doubleValue() != number.longValue()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, errorMessage);
        }
        return number.longValue();
    }

    private Double requiredDouble(Map<?, ?> values, String field, String errorMessage) {
        Object value = values.get(field);
        if (!(value instanceof Number number)) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, errorMessage);
        }
        double doubleValue = number.doubleValue();
        if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, errorMessage);
        }
        return doubleValue;
    }

    private Double optionalDouble(Map<?, ?> values, String field) {
        Object value = values.get(field);
        if (value == null) {
            return null;
        }
        return requiredDouble(values, field, "Knowledge retrieval snippet " + field + " is invalid");
    }

    private String requiredString(Map<?, ?> values, String field, String errorMessage) {
        Object value = values.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, errorMessage);
        }
        return text;
    }

    private long elapsedMs(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
