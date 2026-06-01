package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.dto.AddDepartmentDto;
import com.learning.emsmybatisliquibase.entity.Department;
import com.learning.emsmybatisliquibase.service.DepartmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/department")
@AllArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping("/getAll")
    public ResponseEntity<List<Department>> getAll() {
        return new ResponseEntity<>(departmentService.getAll(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping("/getById/{departmentUuid}")
    public ResponseEntity<Department> getById(@PathVariable UUID departmentUuid) {
        return new ResponseEntity<>(departmentService.getById(departmentUuid), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/addDepartment")
    public ResponseEntity<Department> add(@RequestBody AddDepartmentDto department) {
        return new ResponseEntity<>(departmentService.add(department), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/updateDepartment/{departmentUuid}")
    public ResponseEntity<Department> update(@PathVariable UUID departmentUuid,
                                             @RequestBody AddDepartmentDto department) {
        return new ResponseEntity<>(departmentService.update(departmentUuid, department),
                HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deleteDepartment/{departmentUuid}")
    public ResponseEntity<HttpStatus> delete(@PathVariable UUID departmentUuid) {
        departmentService.delete(departmentUuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/download-department-report")
    public ResponseEntity<byte[]> downloadDepartmentReport() throws IOException {
        var report = departmentService.downloadDepartmentReport();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Employee_Report.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return new ResponseEntity<>(report, headers, HttpStatus.OK);
    }
}

