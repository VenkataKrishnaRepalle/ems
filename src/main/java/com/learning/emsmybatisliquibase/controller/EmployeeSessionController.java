package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.entity.EmployeeSession;
import com.learning.emsmybatisliquibase.service.EmployeeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employeeSession")
public class EmployeeSessionController {

    private final EmployeeSessionService employeeSessionService;

    @PutMapping("/update/{employeeUuid}")
    public ResponseEntity<EmployeeSession> updateEmployeeSession(@PathVariable UUID employeeUuid,
                                                                 @RequestBody EmployeeSession employeeSession) {
        employeeSession.setEmployeeUuid(employeeUuid);
        return ResponseEntity.ok(employeeSessionService.update(employeeUuid, employeeSession));
    }

    @DeleteMapping("/delete/{sessionUuid}")
    public ResponseEntity<Void> deleteEmployeeSession(@PathVariable String sessionUuid) {
        employeeSessionService.deleteBySessionId(sessionUuid);
        return ResponseEntity.noContent().build();
    }
}
