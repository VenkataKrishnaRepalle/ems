package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceType;
import com.learning.emsmybatisliquibase.entity.enums.WorkMode;
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
public class Attendance implements Serializable {

    private UUID uuid;

    private UUID employeeUuid;

    private WorkMode workMode;

    private AttendanceType type;

    private AttendanceStatus status;

    private Date date;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
