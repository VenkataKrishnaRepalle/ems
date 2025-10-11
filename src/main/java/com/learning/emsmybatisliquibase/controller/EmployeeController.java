package com.learning.emsmybatisliquibase.controller;


import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping(value = "/me")
    public ResponseEntity<EmployeeResponseDto> getMe() {
        log.info("Employee Controller:: /me is called");
        return new ResponseEntity<>(employeeService.getMe(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/add")
    public ResponseEntity<AddEmployeeResponseDto> addEmployee(@Valid @RequestBody AddEmployeeDto employeeDto)
            throws MessagingException, UnsupportedEncodingException {
        return new ResponseEntity<>(employeeService.add(employeeDto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping("/getById/{id}")
    public ResponseEntity<Employee> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(employeeService.getById(id), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/getAll")
    public ResponseEntity<List<Employee>> viewAllEmployees() {
        return new ResponseEntity<>(employeeService.getAll(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping("/getAll/pagination")
    public ResponseEntity<PaginatedResponse<Employee>> viewAllEmployees(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "3") int size,
            @RequestParam(name = "sortBy", defaultValue = "uuid") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        return new ResponseEntity<>(employeeService.getAllByPagination(page, size, sortBy, sortOrder),
                HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @PostMapping("/updateLeavingDate/{id}")
    public ResponseEntity<HttpStatus> updateLeavingStatus(@PathVariable UUID id,
                                                          @RequestBody UpdateLeavingDateDto updateLeavingDate) {
        employeeService.updateLeavingDate(id, updateLeavingDate);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping(value = "/getByManagerId/{managerId}")
    public ResponseEntity<List<Employee>> getByManagerId(@PathVariable UUID managerId) {
        return new ResponseEntity<>(employeeService.getByManagerUuid(managerId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping(value = "/getFullTeam/{employeeId}")
    public ResponseEntity<List<EmployeeAndManagerDto>> getFullTeam(@PathVariable UUID employeeId) {
        return new ResponseEntity<>(employeeService.getFullTeam(employeeId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping(value = "getEmployeeFullReportingChain/{employeeId}")
    public ResponseEntity<EmployeeFullReportingChainDto> getEmployeeFullReportingChain(@PathVariable UUID employeeId) {
        return new ResponseEntity<>(employeeService.getEmployeeFullReportingChain(employeeId),
                HttpStatus.OK);
    }

    @GetMapping("/get-active-managers")
    public ResponseEntity<List<EmployeeDetailsDto>> getActiveManagers() {
        return ResponseEntity.ok(employeeService.getAllActiveManagers());
    }

    @GetMapping("/getByNameOrEmail")
    public ResponseEntity<List<EmployeeDetailsDto>> getByNameOrEmail(@RequestParam(name = "name") String name) {
        return ResponseEntity.ok(employeeService.getByNameOrEmail(name));
    }

}
