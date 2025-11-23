package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.LeaveStatus;
import com.learning.emsmybatisliquibase.entity.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Leave implements Serializable {

    private UUID uuid;

    private UUID employeeUuid;

    private Date date;

    private LeaveType type;

    private LeaveStatus status;

    private String reason;

    private String comments;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}