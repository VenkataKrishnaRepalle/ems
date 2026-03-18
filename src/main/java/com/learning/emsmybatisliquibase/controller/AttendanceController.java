package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.dto.AttendanceDto;
import com.learning.emsmybatisliquibase.dto.AttendanceGroupDto;
import com.learning.emsmybatisliquibase.dto.EmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.dto.ViewEmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.entity.Attendance;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;
import com.learning.emsmybatisliquibase.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("api/attendance")
@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @PostMapping("/apply/{employeeUuid}")
    public ResponseEntity<List<Attendance>> apply(@PathVariable UUID employeeUuid, @RequestBody
    List<AttendanceDto> attendanceDto) {
        return new ResponseEntity<>(attendanceService.apply(employeeUuid, attendanceDto),
                HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @PutMapping("/update/{employeeUuid}/attendance/{attendanceUuid}")
    public ResponseEntity<Attendance> update(@PathVariable UUID employeeUuid, @PathVariable UUID attendanceUuid,
                                             @RequestBody AttendanceDto attendanceDto) {
        return new ResponseEntity<>(attendanceService.update(employeeUuid, attendanceUuid, attendanceDto),
                HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/updateByManager/{managerUuid}/attendance/{attendanceUuid}")
    public ResponseEntity<Attendance> updateByManager(@PathVariable UUID managerUuid, @PathVariable UUID attendanceUuid,
                                                      @RequestBody AttendanceDto attendanceDto) {
        return new ResponseEntity<>(attendanceService.updateByManager(managerUuid, attendanceUuid, attendanceDto),
                HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @GetMapping("/get/{employeeUuid}/attendance/{attendanceUuid}")
    public ResponseEntity<Attendance> get(@PathVariable UUID employeeUuid, @PathVariable UUID attendanceUuid) {
        return new ResponseEntity<>(attendanceService.getByUuid(employeeUuid, attendanceUuid),
                HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @GetMapping("/get/timesheet/{employeeUuid}")
    public ResponseEntity<List<AttendanceGroupDto>> getTimesheet(@PathVariable UUID employeeUuid,
                                                                 @RequestParam(name = "year", required = false) Long year,
                                                                 @RequestParam(name = "month", required = false) Integer month) {
        return new ResponseEntity<>(attendanceService.getTimesheet(employeeUuid, year, month),
                HttpStatus.OK);
    }


    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @GetMapping("/get/{employeeUuid}")
    public ResponseEntity<ViewEmployeeAttendanceDto> getEmployeeAttendance(@PathVariable UUID employeeUuid,
                                                                           @RequestParam(name = "year", required = false) Long year,
                                                                           @RequestParam(name = "month", required = false) Integer month) {
        return new ResponseEntity<>(attendanceService.getEmployeeAttendance(employeeUuid, year, month),
                HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @GetMapping("/get/attendance/full-team/{employeeUuid}")
    public ResponseEntity<Map<AttendanceStatus, List<EmployeeAttendanceDto>>> getFullTeamAttendance(@PathVariable UUID employeeUuid,
                                                                                                    @RequestParam(name = "year", required = false) Long year,
                                                                                                    @RequestParam(name = "month", required = false) Integer month) {
        return new ResponseEntity<>(attendanceService.getTeamAttendance(employeeUuid, year, month),
                HttpStatus.OK);
    }
}
