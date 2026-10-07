package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.ProcessExecutionDao;
import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecution;
import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecutionStatus;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.ProcessExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessExecutionServiceImpl implements ProcessExecutionService {

    private final ProcessExecutionDao processExecutionDao;

    @Override
    public ProcessExecution getByProcessInstanceId(Long processInstanceId) {
        var process = processExecutionDao.getByProcessInstanceId(processInstanceId);
        if (process == null) {
            throw new NotFoundException("PROCESS_INSTANCE_NOT_FOUND",
                    "Process instance details not found for id: " + processInstanceId);
        }
        return process;
    }

    @Override
    public List<ProcessExecution> getByEmployeeUuid(UUID employeeUuid) {
        var processes = processExecutionDao.getByEmployeeId(employeeUuid);
        if (processes == null || processes.isEmpty()) {
            throw new NotFoundException("PROCESS_INSTANCE_NOT_FOUND",
                    "Process instance details not found for employee: " + employeeUuid);
        }
        return processes;
    }

    @Override
    public ProcessExecution insert(ProcessExecution processExecution) {
        var process = processExecutionDao.getByProcessInstanceId(processExecution.getProcessInstanceKey());
        if (process != null) {
            throw new FoundException("PROCESS_INSTANCE_FOUND",
                    "Process instance details not found for id: " + processExecution.getProcessInstanceKey());
        }

        processExecution.setStartedTime(LocalDateTime.now());
        processExecution.setStatus(ProcessExecutionStatus.IN_PROGRESS);

        try {
            if (0 == processExecutionDao.insert(processExecution)) {
                throw new IntegrityException("PROCESS_INSTANCE_INSERT_FAILED", "Failed to insert process execution");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_INSTANCE_INSERT_FAILED", exception.getCause().getMessage());
        }

        return processExecution;
    }

    @Override
    public ProcessExecution update(ProcessExecution processExecution) {
        var process = processExecutionDao.getByProcessInstanceId(processExecution.getProcessInstanceKey());
        if (process == null) {
            return insert(processExecution);
        }
        processExecution.setCompletedTime(LocalDateTime.now());
        try {
            if (0 == processExecutionDao.update(processExecution)) {
                throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED", "Failed to update process execution");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED", exception.getCause().getMessage());
        }

        return processExecution;
    }

    @Override
    public void updateEmployee(Long processInstanceId, UUID employeeUuid) {
        try {
            if (0 == processExecutionDao.updateEmployee(processInstanceId, employeeUuid)) {
                throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED",
                        "Failed to update process execution for id: " + processInstanceId);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED", exception.getCause().getMessage());
        }
    }

    @Override
    public void updateStatus(Long processInstanceId, ProcessExecutionStatus status) {
        try {
            if (0 == processExecutionDao.updateStatus(processInstanceId, status)) {
                throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED",
                        "Failed to update process execution for id: " + processInstanceId);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED", exception.getCause().getMessage());
        }
    }

    @Override
    public void updateErrorDetails(Long processInstanceId, String failedStep, String errorCode, String errorMessage) {
        var process = processExecutionDao.getByProcessInstanceId(processInstanceId);
        if (process == null) {
            throw new NotFoundException("PROCESS_INSTANCE_NOT_FOUND",
                    "Process instance details not found for id: " + processInstanceId);
        }
        process.setStatus(ProcessExecutionStatus.FAILED);
        process.setFailedStep(failedStep);
        process.setErrorCode(errorCode);
        process.setErrorMessage(errorMessage);
        process.setCompletedTime(LocalDateTime.now());
        try {
            if (0 == processExecutionDao.update(process)) {
                throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED", "Failed to update process execution");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_INSTANCE_UPDATE_FAILED", exception.getCause().getMessage());
        }
    }

    @Override
    public void deleteByProcessId(Long id) {
        try {
            if (0 == processExecutionDao.deleteByProcessId(id)) {
                throw new IntegrityException("PROCESS_DELETE_FAILED", "Failed to delete process execution for id: " + id);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_DELETE_FAILED", exception.getCause().getMessage());
        }
    }

    @Override
    public void deleteByEmployeeId(UUID id) {
        try {
            if (0 == processExecutionDao.deleteByEmployeeId(id)) {
                throw new IntegrityException("PROCESS_DELETE_FAILED", "Failed to delete process execution for employee: " + id);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PROCESS_DELETE_FAILED", exception.getCause().getMessage());
        }
    }
}
