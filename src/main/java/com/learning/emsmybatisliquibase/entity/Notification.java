package com.learning.emsmybatisliquibase.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {

    private UUID uuid;

    private UUID employeeUuid;

    private String title;

    private String message;

    private String link;

    private Status status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    public enum Status {
        READ, UNREAD
    }

    public boolean isRead() {
        return this.status == Status.READ;
    }

    public boolean isUnread() {
        return this.status == Status.UNREAD;
    }

    public Notification(UUID employeeUuid, String title, String message, String link, Status status) {
        this.uuid = UUID.randomUUID();
        this.employeeUuid = employeeUuid;
        this.title = title;
        this.message = message;
        this.link = link;
        this.status = status;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }
}
