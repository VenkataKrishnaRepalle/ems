package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.AttendanceDto;
import com.learning.emsmybatisliquibase.dto.AttendanceGroupDto;
import com.learning.emsmybatisliquibase.dto.EmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.dto.ViewEmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.entity.Attendance;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AttendanceService {
    List<Attendance> apply(UUID employeeUuid, List<AttendanceDto> attendanceDto);

    Attendance update(UUID employeeUuid, UUID attendanceUuid, AttendanceDto attendanceDto);

    Attendance getByUuid(UUID employeeUuid, UUID attendanceUuid);

    ViewEmployeeAttendanceDto getEmployeeAttendance(UUID employeeUuid, Long year, Integer month);

    Map<AttendanceStatus, List<EmployeeAttendanceDto>> getTeamAttendance(UUID employeeUuid, Long year, Integer month);

    Attendance updateByManager(UUID managerUuid, UUID attendanceUuid, AttendanceDto attendanceDto);

    List<AttendanceGroupDto> getTimesheet(UUID employeeUuid, Long year, Integer month);
}
