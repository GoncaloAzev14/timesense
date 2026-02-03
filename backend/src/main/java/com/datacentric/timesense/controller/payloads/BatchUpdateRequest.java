package com.datacentric.timesense.controller.payloads;

import java.util.List;

public class BatchUpdateRequest {
    private List<Long> timeRecordIds;
    private Long projectId;
    private Long taskId;

    public List<Long> getTimeRecordIds() {
        return timeRecordIds;
    }

    public void setTimeRecordIds(List<Long> timeRecordIds) {
        this.timeRecordIds = timeRecordIds;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
