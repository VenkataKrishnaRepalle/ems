package com.learning.emsmybatisliquibase.entity.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Audit {

    private UUID uuid;

    private UUID employeeUuid;

    private String entityType;

    private String action;

    private Map<String, Map<String, String>> changes;

    private String changesMessage;

    private LocalDateTime createdTime;

    private UUID createdBy;
}
