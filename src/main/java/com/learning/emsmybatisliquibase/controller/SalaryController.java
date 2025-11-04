package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.entity.Salary;
import com.learning.emsmybatisliquibase.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController("salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping("/process-salary")
    public ResponseEntity<Salary> processSalary(@RequestParam("type") String type,
                                                @RequestParam("amount") String amount,
                                                @RequestParam(value = "joiningBonus", required = false) String joiningBonus) {
        return new ResponseEntity<>(salaryService.process(type, amount, joiningBonus), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PostMapping("/add")
    public ResponseEntity<Salary> insert(@RequestBody Salary salary) {
        return new ResponseEntity<>(salaryService.insert(salary), HttpStatus.CREATED);
    }

    @GetMapping("/get/salary/{employeeUuid}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable("employeeUuid") UUID employeeUuid) {
        return new ResponseEntity<>(salaryService.get(employeeUuid), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PutMapping("/update/salary/{employeeUuid}")
    public ResponseEntity<Salary> update(@PathVariable("employeeUuid") UUID employeeUuid,
                                         @RequestParam(name = "percentage") String percentage) {
        return new ResponseEntity<>(salaryService.update(employeeUuid, percentage), HttpStatus.OK);
    }
}
