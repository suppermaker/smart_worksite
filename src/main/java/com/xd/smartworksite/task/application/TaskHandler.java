package com.xd.smartworksite.task.application;

import com.xd.smartworksite.task.domain.TaskExecutionContext;

public interface TaskHandler {

    String taskType();

    void handle(TaskExecutionContext context);
}
