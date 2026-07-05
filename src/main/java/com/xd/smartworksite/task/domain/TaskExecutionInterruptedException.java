package com.xd.smartworksite.task.domain;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;

public class TaskExecutionInterruptedException extends BusinessException {

    public TaskExecutionInterruptedException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
