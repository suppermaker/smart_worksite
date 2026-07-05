package com.xd.smartworksite.knowledge.infra;

import org.springframework.http.client.ClientHttpRequestFactory;

import java.time.Duration;

public interface KnowledgeRetrievalRequestFactoryProvider {

    ClientHttpRequestFactory requestFactory(Duration timeout);
}
