package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.entity.EmployeeSession;
import com.learning.emsmybatisliquibase.service.EmployeeSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employeeSession")
public class EmployeeSessionController {

    private final EmployeeSessionService employeeSessionService;

    @GetMapping("/get/{email}")
    public ResponseEntity<Map<String, List<EmployeeSession>>> getByEmployeeUuid(@PathVariable String email,
                                                                                @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(employeeSessionService.get(email, isActive));
    }

    @PutMapping("/update/{employeeUuid}")
    public ResponseEntity<EmployeeSession> updateEmployeeSession(@PathVariable UUID employeeUuid,
                                                                 @RequestBody EmployeeSession employeeSession) {
        employeeSession.setEmployeeUuid(employeeUuid);
        return ResponseEntity.ok(employeeSessionService.update(employeeUuid, employeeSession));
    }

    @DeleteMapping("/delete/{sessionUuid}")
    public ResponseEntity<Void> deleteEmployeeSession(@PathVariable UUID sessionUuid) {
        employeeSessionService.delete(sessionUuid);
        return ResponseEntity.noContent().build();
    }
}
