package com.learning.emsmybatisliquibase.entity.camunda;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessExecution {

    private Long processInstanceKey;

    private UUID employeeUuid;

    private String processDefinitionId;

    private String processName;

    private String businessKey;

    private ProcessExecutionStatus status;

    private String failedStep;

    private String errorCode;

    private String errorMessage;

    private UUID startedBy;

    private LocalDateTime startedTime;

    private LocalDateTime completedTime;

}
