package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.AttendanceDao;
import com.learning.emsmybatisliquibase.dto.AttendanceDto;
import com.learning.emsmybatisliquibase.dto.ViewEmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.entity.Attendance;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.AttendanceMapper;
import com.learning.emsmybatisliquibase.service.AttendanceService;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.learning.emsmybatisliquibase.exception.errorcodes.AttendanceErrorCodes.*;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.MANAGER_ACCESS_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceDao attendanceDao;

    private final EmployeeService employeeService;

    private final AttendanceMapper attendanceMapper;

    private static final String DATE_FORMAT = "MM-dd-yyyy";

    @Override
    public List<Attendance> apply(UUID employeeUuid, List<AttendanceDto> attendanceDtos) {
        var appliedAttendances = attendanceDao.getByEmployeeUuid(employeeUuid, null, null);

        for (var attendanceDto : attendanceDtos) {
            var formattedDate = new SimpleDateFormat(DATE_FORMAT).format(attendanceDto.getDate());
            for (var attendance : appliedAttendances) {
                if (formattedDate.equals(new SimpleDateFormat(DATE_FORMAT).format(attendance.getDate()))) {
                    throw new FoundException(ATTENDANCE_ALREADY_EXISTS.code(),
                            "Attendance already applied for date " + formattedDate);
                }
            }
        }

        var attendances = attendanceDtos.stream()
                .map(attendanceMapper::applyAttendanceDtoToAttendance)
                .toList();
        attendances.forEach(attendance -> {
            attendance.setUuid(UUID.randomUUID());
            attendance.setEmployeeUuid(employeeUuid);
            attendance.setStatus(AttendanceStatus.SUBMITTED);
            attendance.setCreatedTime(LocalDateTime.now());
            attendance.setUpdatedTime(LocalDateTime.now());
        });
        attendances.forEach(attendance -> {
            try {
                if (0 == attendanceDao.insert(attendance)) {
                    throw new IntegrityException(ATTENDANCE_NOT_CREATED.code(), "Attendance not created");
                }
            } catch (DataIntegrityViolationException exception) {
                throw new IntegrityException(ATTENDANCE_NOT_CREATED.code(), exception.getCause().getMessage());
            }
        });
        return attendances;
    }

    @Override
    public Attendance update(UUID employeeUuid, UUID attendanceUuid, AttendanceDto attendanceDto) {
        var attendance = getByUuid(employeeUuid, attendanceUuid);
        return updateAttendance(attendanceDto, attendance);
    }

    @Override
    public Attendance updateByManager(UUID managerUuid, UUID attendanceUuid, AttendanceDto attendanceDto) {
        var attendance = get(attendanceUuid);
        var employee = employeeService.getById(attendance.getEmployeeUuid());
        if (!employee.getManagerUuid().equals(managerUuid)) {
            throw new NotFoundException(MANAGER_ACCESS_NOT_FOUND.code(), "User is not a Manager");
        }
        return updateAttendance(attendanceDto, attendance);
    }

    private Attendance updateAttendance(AttendanceDto attendanceDto, Attendance attendance) {
        attendance.setWorkMode(attendanceDto.getWorkMode());
        attendance.setType(attendanceDto.getType());
        attendance.setStatus(attendanceDto.getStatus());

        try {
            if (0 == attendanceDao.update(attendance)) {
                throw new InvalidInputException(ATTENDANCE_NOT_UPDATED.code(), "Attendance Failed to Update");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(ATTENDANCE_NOT_UPDATED.code(), exception.getCause().getMessage());
        }

        return attendance;
    }

    @Override
    public Attendance getByUuid(UUID employeeUuid, UUID attendanceUuid) {
        var attendance = get(attendanceUuid);
        if (!attendance.getEmployeeUuid().equals(employeeUuid)) {
            throw new InvalidInputException(ATTENDANCE_NOT_EXISTS.code(),
                    "Attendance not exists for employeeId: " + employeeUuid + " and attendanceId: " + attendanceUuid);
        }
        return attendance;
    }

    private Attendance get(UUID id) {
        var attendance = attendanceDao.getById(id);
        if (attendance == null) {
            throw new InvalidInputException(ATTENDANCE_NOT_EXISTS.code(), "Attendance not exists for attendanceId: " + id);
        }
        return attendance;
    }

    @Override
    public ViewEmployeeAttendanceDto getEmployeeAttendance(UUID employeeUuid, Long year, Integer month) {
        if (year == null) {
            year = (long) LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonth().getValue();
        }
        var employee = employeeService.getById(employeeUuid);

        var attendances = attendanceDao.getByEmployeeUuid(employeeUuid, year, month);

        Map<AttendanceStatus, List<Attendance>> attendanceStatusListMap = new EnumMap<>(AttendanceStatus.class);

        for (AttendanceStatus status : AttendanceStatus.values()) {
            attendanceStatusListMap.put(status, filterAttendanceByStatus(attendances, status));
        }

        return ViewEmployeeAttendanceDto.builder()
                .employeeUuid(employee.getUuid())
                .employeeFirstName(employee.getFirstName())
                .employeeLastName(employee.getLastName())
                .submitted(attendanceStatusListMap.get(AttendanceStatus.SUBMITTED))
                .approved(attendanceStatusListMap
                        .get(AttendanceStatus.APPROVED))
                .cancelled(attendanceStatusListMap.get(AttendanceStatus.CANCELLED))
                .build();
    }

    private List<Attendance> filterAttendanceByStatus(List<Attendance> attendances, AttendanceStatus status) {
        return attendances.stream()
                .filter(attendance -> attendance.getStatus().equals(status))
                .toList();
    }

    @Override
    public List<ViewEmployeeAttendanceDto> getTeamAttendance(UUID employeeUuid, Long year, Integer month) {
        if (year == null) {
            year = (long) LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonth().getValue();
        }
        var fullTeam = employeeService.getByManagerUuid(employeeUuid);
        List<ViewEmployeeAttendanceDto> employeeAttendance = new ArrayList<>();
        for (var employee : fullTeam) {
            employeeAttendance.add(getEmployeeAttendance(employee.getUuid(), year, month));
        }
        return employeeAttendance;
    }
}
