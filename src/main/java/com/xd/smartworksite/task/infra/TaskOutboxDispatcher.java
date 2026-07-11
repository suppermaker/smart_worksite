package com.xd.smartworksite.task.infra;

import com.xd.smartworksite.task.application.TaskOutboxApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(TaskOutboxDispatcherProperties.class)
@ConditionalOnProperty(prefix = "app.task.outbox.dispatcher", name = "enabled", havingValue = "true")
public class TaskOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(TaskOutboxDispatcher.class);

    private final TaskOutboxApplicationService outboxApplicationService;
    private final TaskOutboxDispatcherProperties properties;

    public TaskOutboxDispatcher(TaskOutboxApplicationService outboxApplicationService,
                                TaskOutboxDispatcherProperties properties) {
        this.outboxApplicationService = outboxApplicationService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.task.outbox.dispatcher.fixed-delay-ms:5000}")
    public void dispatch() {
        int delivered = outboxApplicationService.deliverDueEvents(properties.getBatchSize());
        if (delivered > 0) {
            log.info("task outbox dispatch completed, delivered={}", delivered);
        }
    }
}
