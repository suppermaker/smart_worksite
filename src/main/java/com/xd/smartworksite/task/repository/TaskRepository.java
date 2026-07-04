package com.xd.smartworksite.task.repository;

import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.task.domain.TaskStageLog;
import com.xd.smartworksite.task.domain.TaskStageCode;
import com.xd.smartworksite.task.domain.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Optional<GenerateTask> findTaskById(Long taskId);

    void create(GenerateTask task);

    boolean updateStatus(GenerateTask task, TaskStatus expectedStatus, TaskStatus nextStatus, TaskStageCode nextStage, String errorMessage);

    boolean retry(GenerateTask task, TaskStatus expectedStatus);

    List<TaskStageLog> findStageLogsByTaskId(Long taskId);

    void saveStageLog(TaskStageLog stageLog);
}
