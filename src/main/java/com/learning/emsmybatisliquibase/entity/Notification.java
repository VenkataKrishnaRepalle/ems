package com.learning.emsmybatisliquibase.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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

    private Instant createdTime;

    private Instant updatedTime;

    public enum Status {
        READ, UNREAD
    }

    public void isRead() {
        this.status = Status.READ;
    }

    public void isUnread() {
        this.status = Status.UNREAD;
    }
}
