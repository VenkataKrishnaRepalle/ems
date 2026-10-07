package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecution;
import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecutionStatus;

import java.util.List;
import java.util.UUID;

public interface ProcessExecutionService {

    ProcessExecution getByProcessInstanceId(Long processInstanceId);

    List<ProcessExecution> getByEmployeeUuid(UUID employeeUuid);

    ProcessExecution insert(ProcessExecution processExecution);

    ProcessExecution update(ProcessExecution processExecution);

    void updateEmployee(Long processInstanceId, UUID employeeUuid);

    void updateStatus(Long processInstanceId, ProcessExecutionStatus status);

    void updateErrorDetails(Long processInstanceId, String failedStep, String errorCode, String errorMessage);

    void deleteByProcessId(Long id);

    void deleteByEmployeeId(UUID id);
}
