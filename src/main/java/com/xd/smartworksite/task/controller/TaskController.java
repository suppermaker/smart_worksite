package com.xd.smartworksite.task.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.task.application.TaskApplicationService;
import com.xd.smartworksite.task.dto.TaskQueryRequest;
import com.xd.smartworksite.task.dto.TaskResponse;
import com.xd.smartworksite.task.dto.TaskStageLogResponse;
import com.xd.smartworksite.task.dto.TaskStatisticsResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Validated
public class TaskController {
    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResult<TaskResponse>> listTasks(@Valid TaskQueryRequest request) {
        return ApiResponse.success(taskApplicationService.queryTasks(request));
    }

    @GetMapping("/statistics")
    public ApiResponse<TaskStatisticsResponse> statistics(@RequestParam(required = false) Long projectId) {
        return ApiResponse.success(taskApplicationService.statistics(projectId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.getTask(taskId));
    }

    @GetMapping("/{taskId}/stages")
    public ApiResponse<List<TaskStageLogResponse>> getStages(@PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.getStages(taskId));
    }

    @PostMapping("/{taskId}/retry")
    public ApiResponse<TaskResponse> retry(@PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.retryTask(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskResponse> cancel(@PathVariable Long taskId) {
        return ApiResponse.success(taskApplicationService.cancelTask(taskId));
    }
}
