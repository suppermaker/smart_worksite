package com.xd.smartworksite.knowledge.infra;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SimpleKnowledgeRetrievalRequestFactoryProvider implements KnowledgeRetrievalRequestFactoryProvider {

    @Override
    public ClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }
}
