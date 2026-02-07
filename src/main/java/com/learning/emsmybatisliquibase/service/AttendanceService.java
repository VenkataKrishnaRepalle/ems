package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.AttendanceDto;
import com.learning.emsmybatisliquibase.dto.ViewEmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.entity.Attendance;

import java.util.List;
import java.util.UUID;

public interface AttendanceService {
    List<Attendance> apply(UUID employeeUuid, List<AttendanceDto> attendanceDto);

    Attendance update(UUID employeeUuid, UUID attendanceUuid, AttendanceDto attendanceDto);

    Attendance getByUuid(UUID employeeUuid, UUID attendanceUuid);

    ViewEmployeeAttendanceDto getEmployeeAttendance(UUID employeeUuid, Long year);

    List<ViewEmployeeAttendanceDto> getTeamAttendance(UUID employeeUuid, Long year);

    Attendance updateByManager(UUID managerUuid, UUID attendanceUuid, AttendanceDto attendanceDto);
}
