package com.xd.smartworksite.datasource.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.datasource.application.DatabaseQuestionApplicationService;
import com.xd.smartworksite.datasource.dto.DatabaseQueryRequest;
import com.xd.smartworksite.datasource.dto.DatabaseQueryResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/database")
@Validated
public class DatabaseQuestionController {

    private final DatabaseQuestionApplicationService databaseQuestionApplicationService;

    public DatabaseQuestionController(DatabaseQuestionApplicationService databaseQuestionApplicationService) {
        this.databaseQuestionApplicationService = databaseQuestionApplicationService;
    }

    @PostMapping("/query")
    public ApiResponse<DatabaseQueryResponse> query(@Valid @RequestBody DatabaseQueryRequest request) {
        return ApiResponse.success(databaseQuestionApplicationService.query(request));
    }
}
