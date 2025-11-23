package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Period implements Serializable {

    private UUID uuid;

    private String name;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private PeriodStatus status;

    private UUID createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
