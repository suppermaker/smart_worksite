package com.xd.smartworksite.task.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.task.application.TaskApplicationService;
import com.xd.smartworksite.task.dto.TaskResponse;
import com.xd.smartworksite.task.dto.TaskStageLogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@RequestParam Long projectId, @PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.getTask(projectId, taskId));
    }

    @GetMapping("/{taskId}/stages")
    public ApiResponse<List<TaskStageLogResponse>> getTaskStages(@RequestParam Long projectId, @PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.getStageLogs(projectId, taskId));
    }

    @PostMapping("/{taskId}/retry")
    public ApiResponse<TaskResponse> retryTask(@RequestParam Long projectId, @PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.retryTask(projectId, taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskResponse> cancelTask(@RequestParam Long projectId, @PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.cancelTask(projectId, taskId));
    }
}
