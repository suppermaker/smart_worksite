package com.xd.smartworksite.knowledge.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeRetrievalProperties.class)
public class KnowledgeRetrievalConfig {
}
