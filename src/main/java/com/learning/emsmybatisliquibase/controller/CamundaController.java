package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecution;
import com.learning.emsmybatisliquibase.service.ProcessExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/camunda")
public class CamundaController {

    private final ProcessExecutionService processExecutionService;

    @GetMapping("/get/{processKey}")
    public ResponseEntity<ProcessExecution> getProcessExecution(@PathVariable Long processKey) {
        return ResponseEntity.ok(processExecutionService.getByProcessInstanceId(processKey));
    }

    @GetMapping("/get/employee/{employeeUuid}")
    public ResponseEntity<List<ProcessExecution>> getByEmployeeUuid(@PathVariable UUID employeeUuid) {
        return ResponseEntity.ok(processExecutionService.getByEmployeeUuid(employeeUuid));
    }

    @PostMapping("/add")
    public ResponseEntity<ProcessExecution> addProcessExecution(@RequestBody ProcessExecution processExecution) {
        return ResponseEntity.ok(processExecutionService.insert(processExecution));
    }

    @PutMapping("/update/{processKey}")
    public ResponseEntity<ProcessExecution> updateProcessExecution(@PathVariable Long processKey,
                                                                   @RequestBody ProcessExecution processExecution) {
        return ResponseEntity.ok(processExecutionService.update(processExecution));
    }

    @DeleteMapping("/delete/{processKey}")
    public ResponseEntity<ProcessExecution> deleteProcessExecution(@PathVariable Long processKey) {
        processExecutionService.deleteByProcessId(processKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/employee/{employeeUuid}")
    public ResponseEntity<ProcessExecution> deleteProcessExecution(@PathVariable UUID employeeUuid) {
        processExecutionService.deleteByEmployeeId(employeeUuid);
        return ResponseEntity.noContent().build();
    }
}
