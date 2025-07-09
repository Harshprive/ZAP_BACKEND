package com.ZAP_Backend.ZapServices.DataTransferObject;

import java.time.LocalDateTime;

public class ScheduleDateRequest {
    private LocalDateTime scheduledDateTime;

    public ScheduleDateRequest() {
    }

    public ScheduleDateRequest(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }
} 