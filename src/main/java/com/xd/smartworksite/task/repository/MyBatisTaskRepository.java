package com.xd.smartworksite.task.repository;

import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.task.domain.TaskStageCode;
import com.xd.smartworksite.task.domain.TaskStageLog;
import com.xd.smartworksite.task.domain.TaskStatus;
import com.xd.smartworksite.task.mapper.TaskMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisTaskRepository implements TaskRepository {

    private final TaskMapper taskMapper;

    public MyBatisTaskRepository(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public Optional<GenerateTask> findTaskById(Long taskId) {
        return Optional.ofNullable(taskMapper.selectTaskById(taskId));
    }

    @Override
    public boolean updateStatus(GenerateTask task, TaskStatus nextStatus, TaskStageCode nextStage, String errorMessage) {
        boolean markStarted = nextStatus == TaskStatus.RUNNING && task.getStartedAt() == null;
        boolean markFinished = nextStatus == TaskStatus.SUCCESS || nextStatus == TaskStatus.FAILED || nextStatus == TaskStatus.CANCELED;
        int updated = taskMapper.updateTaskStatus(
                task.getId(),
                task.getStatus().name(),
                nextStatus.name(),
                nextStage == null ? null : nextStage.name(),
                errorMessage,
                markStarted,
                markFinished
        );
        return updated == 1;
    }

    @Override
    public boolean retry(GenerateTask task) {
        int updated = taskMapper.retryTask(task.getId(), task.getStatus().name(), TaskStatus.RETRYING.name());
        return updated == 1;
    }

    @Override
    public List<TaskStageLog> findStageLogsByTaskId(Long taskId) {
        return taskMapper.selectStageLogsByTaskId(taskId);
    }

    @Override
    public void saveStageLog(TaskStageLog stageLog) {
        taskMapper.insertStageLog(stageLog);
    }
}
