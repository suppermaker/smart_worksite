package com.xd.smartworksite.task.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TaskHandlerRegistry {

    private final Map<String, TaskHandler> handlers;

    public TaskHandlerRegistry(List<TaskHandler> taskHandlers) {
        Map<String, TaskHandler> mappedHandlers = new HashMap<>();
        for (TaskHandler handler : taskHandlers) {
            TaskHandler previous = mappedHandlers.putIfAbsent(handler.taskType(), handler);
            if (previous != null) {
                throw new BusinessException(ErrorCode.CONFLICT, "Duplicate task handler for type " + handler.taskType());
            }
        }
        this.handlers = Map.copyOf(mappedHandlers);
    }

    public Optional<TaskHandler> findHandler(String taskType) {
        return Optional.ofNullable(handlers.get(taskType));
    }
}
