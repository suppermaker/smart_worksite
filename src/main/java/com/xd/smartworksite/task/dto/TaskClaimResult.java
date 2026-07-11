package com.xd.smartworksite.task.dto;

import com.xd.smartworksite.task.domain.GenerateTask;

public class TaskClaimResult {
    private final boolean claimed;
    private final GenerateTask task;

    private TaskClaimResult(boolean claimed, GenerateTask task) {
        this.claimed = claimed;
        this.task = task;
    }

    public static TaskClaimResult claimed(GenerateTask task) {
        return new TaskClaimResult(true, task);
    }

    public static TaskClaimResult notClaimed() {
        return new TaskClaimResult(false, null);
    }

    public boolean isClaimed() {
        return claimed;
    }

    public GenerateTask getTask() {
        return task;
    }
}
