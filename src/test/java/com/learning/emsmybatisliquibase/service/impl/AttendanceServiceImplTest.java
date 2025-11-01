package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.AttendanceDao;
import com.learning.emsmybatisliquibase.dto.ApplyAttendanceDto;
import com.learning.emsmybatisliquibase.dto.EmployeeAndManagerDto;
import com.learning.emsmybatisliquibase.dto.UpdateAttendanceDto;
import com.learning.emsmybatisliquibase.dto.ViewEmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.entity.*;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceType;
import com.learning.emsmybatisliquibase.entity.enums.WorkMode;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.mapper.AttendanceMapper;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @InjectMocks
    AttendanceServiceImpl attendanceService;

    @Mock
    AttendanceDao attendanceDao;

    @Mock
    AttendanceMapper attendanceMapper;

    @Mock
    EmployeeService employeeService;

    private static final UUID EMPLOYEE_UUID = UUID.randomUUID();

    @Test
    void testApply_AttendanceAlreadyExists() {
        var attendanceDto = new ApplyAttendanceDto();
        attendanceDto.setDate(Date.from(Instant.now()));
        List<ApplyAttendanceDto> attendanceDtos = List.of(attendanceDto);

        var existingAttendance = new Attendance();
        existingAttendance.setDate(attendanceDto.getDate());

        when(attendanceDao.getByEmployeeUuid(EMPLOYEE_UUID)).thenReturn(List.of(existingAttendance));

        assertThrows(FoundException.class, () ->
                attendanceService.apply(EMPLOYEE_UUID, attendanceDtos));

        verify(attendanceDao, times(1)).getByEmployeeUuid(EMPLOYEE_UUID);
    }

    @Test
    void testApplyAttendanceNotCreated() {
        ApplyAttendanceDto attendanceDto = new ApplyAttendanceDto(WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()));
        List<ApplyAttendanceDto> attendanceDtos = List.of(attendanceDto);
        when(attendanceDao.getByEmployeeUuid(EMPLOYEE_UUID)).thenReturn(Collections.emptyList());
        when(attendanceMapper.applyAttendanceDtoToAttendance(attendanceDto)).thenReturn(new Attendance());
        when(attendanceDao.insert(any(Attendance.class))).thenReturn(0);

        assertThrows(IntegrityException.class, () ->
                attendanceService.apply(EMPLOYEE_UUID, attendanceDtos));

        verify(attendanceDao, times(1)).getByEmployeeUuid(EMPLOYEE_UUID);
        verify(attendanceMapper, times(1)).applyAttendanceDtoToAttendance(attendanceDto);
        verify(attendanceDao, times(1)).insert(any());
    }


    @Test
    void testApply_success() {
        UUID employeeUuid = UUID.randomUUID();
        List<ApplyAttendanceDto> attendanceDtos = List.of(
                new ApplyAttendanceDto(WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now())),
                new ApplyAttendanceDto(WorkMode.WORK_FROM_HOME, AttendanceType.HALF_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()))
        );

        when(attendanceDao.getByEmployeeUuid(employeeUuid)).thenReturn(List.of());
        when(attendanceMapper.applyAttendanceDtoToAttendance(any(ApplyAttendanceDto.class))).thenAnswer(invocation -> {
            ApplyAttendanceDto applyAttendanceDto = invocation.getArgument(0);
            return Attendance.builder()
                    .date(applyAttendanceDto.getDate())
                    .workMode(applyAttendanceDto.getWorkMode())
                    .type(applyAttendanceDto.getType())
                    .build();
        });
        when(attendanceDao.insert(any(Attendance.class))).thenReturn(1);

        List<Attendance> result = attendanceService.apply(employeeUuid, attendanceDtos);

        assertThat(result).hasSize(2);
        verify(attendanceDao, times(2)).insert(any(Attendance.class));
        verify(attendanceDao, times(1)).getByEmployeeUuid(any());
        verifyNoMoreInteractions(attendanceDao);
    }

    @Test
    void testGetByUuid_success() {
        UUID uuid = UUID.randomUUID();
        Attendance attendance = new Attendance(uuid, EMPLOYEE_UUID, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now());

        when(attendanceDao.getById(uuid)).thenReturn(attendance);
        assertEquals(EMPLOYEE_UUID, attendance.getEmployeeUuid());

        Attendance result = attendanceService.getByUuid(EMPLOYEE_UUID, uuid);
        assertNotNull(result);
        assertEquals(attendance, result);
        verify(attendanceDao, times(1)).getById(uuid);
    }

    @Test
    void testGetByUuid_attendanceNull() {
        when(attendanceDao.getById(any(UUID.class))).thenReturn(null);
        assertThrows(InvalidInputException.class, () ->
                attendanceService.getByUuid(EMPLOYEE_UUID, UUID.randomUUID()));

        verify(attendanceDao, times(1)).getById(any(UUID.class));
    }

    @Test
    void testGetByUuid_attendanceNotMatchWithEmployee() {
        UUID uuid = UUID.randomUUID();
        Attendance attendance = new Attendance(uuid, UUID.randomUUID(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now());

        when(attendanceDao.getById(uuid)).thenReturn(attendance);
        assertNotEquals(EMPLOYEE_UUID, attendance.getEmployeeUuid());

        assertThrows(InvalidInputException.class, () ->
                attendanceService.getByUuid(EMPLOYEE_UUID, uuid));

        verify(attendanceDao, times(1)).getById(uuid);
    }

    @Test
    void testUpdate_success() {
        UUID uuid = UUID.randomUUID();
        Attendance attendance = new Attendance(uuid, EMPLOYEE_UUID, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now());
        UpdateAttendanceDto attendanceDto = new UpdateAttendanceDto(uuid, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED);

        when(attendanceDao.getById(uuid)).thenReturn(attendance);
        when(attendanceDao.update(attendance)).thenReturn(1);

        attendanceService.update(EMPLOYEE_UUID, uuid, attendanceDto);

        verify(attendanceDao, times(1)).getById(uuid);
        verify(attendanceDao, times(1)).update(attendance);
    }

    @Test
    void testUpdate_notUpdated() {
        UUID uuid = UUID.randomUUID();
        Attendance attendance = new Attendance(uuid, EMPLOYEE_UUID, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now());
        UpdateAttendanceDto attendanceDto = new UpdateAttendanceDto(uuid, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED);

        when(attendanceDao.getById(uuid)).thenReturn(attendance);
        when(attendanceDao.update(any(Attendance.class))).thenReturn(0);

        assertThrows(InvalidInputException.class, () ->
                attendanceService.update(EMPLOYEE_UUID, uuid, attendanceDto));

        verify(attendanceDao, times(1)).getById(uuid);
        verify(attendanceDao, times(1)).update(attendance);
    }

    @Test
    void getEmployeeAttendance_success() {
        Employee employee = new Employee();
        employee.setUuid(EMPLOYEE_UUID);
        employee.setFirstName("test");
        employee.setLastName("test last");
        List<Attendance> attendanceList = List.of(
                new Attendance(UUID.randomUUID(), EMPLOYEE_UUID, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now()),
                new Attendance(UUID.randomUUID(), EMPLOYEE_UUID, WorkMode.WORK_FROM_HOME, AttendanceType.HALF_DAY, AttendanceStatus.WAITING_FOR_CANCELLATION, Date.from(Instant.now()), Instant.now(), Instant.now()),
                new Attendance(UUID.randomUUID(), EMPLOYEE_UUID, WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.CANCELLED, Date.from(Instant.now()), Instant.now(), Instant.now()));

        when(employeeService.getById(EMPLOYEE_UUID)).thenReturn(employee);
        when(attendanceDao.getByEmployeeUuid(EMPLOYEE_UUID)).thenReturn(attendanceList);

        ViewEmployeeAttendanceDto response = attendanceService.getEmployeeAttendance(EMPLOYEE_UUID);

        assertNotNull(response);
        assertEquals(employee.getUuid(), response.getEmployeeUuid());
        assertEquals(employee.getFirstName(), response.getEmployeeFirstName());
        assertEquals(employee.getLastName(), response.getEmployeeLastName());
        assertEquals(1, response.getSubmittedAttendance().size());
        assertEquals(1, response.getWaitingForCancellationAttendance().size());
        assertEquals(1, response.getCancelledAttendance().size());

        verify(employeeService, times(1)).getById(EMPLOYEE_UUID);
        verify(attendanceDao, times(1)).getByEmployeeUuid(EMPLOYEE_UUID);
    }

//    @Test
//    void getAllEmployeesAttendanceByManager_success() {
//        UUID managerUuid = UUID.randomUUID();
//
//        Employee employee1 = Employee.builder()
//                .uuid(UUID.randomUUID())
//                .firstName("test1")
//                .lastName("test1")
//                .managerUuid(managerUuid)
//                .build();
//        Employee employee2 = Employee.builder()
//                .uuid(UUID.randomUUID())
//                .firstName("test2")
//                .lastName("test2")
//                .managerUuid(managerUuid)
//                .build();
//        List<Employee> employees = List.of(employee1, employee2);
//
//        List<Attendance> attendanceList1 = List.of(
//                new Attendance(UUID.randomUUID(), employee1.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee1.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.HALF_DAY, AttendanceStatus.WAITING_FOR_CANCELLATION, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee1.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.CANCELLED, Date.from(Instant.now()), Instant.now(), Instant.now()));
//
//        List<Attendance> attendanceList2 = List.of(
//                new Attendance(UUID.randomUUID(), employee2.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee2.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.HALF_DAY, AttendanceStatus.WAITING_FOR_CANCELLATION, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee2.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.CANCELLED, Date.from(Instant.now()), Instant.now(), Instant.now()));
//
//        assertDoesNotThrow(() -> employeeService.isManager(managerUuid));
//
//        when(employeeService.getByManagerUuid(managerUuid)).thenReturn(employees);
//        when(employeeService.getById(employee1.getUuid())).thenReturn(employee1);
//        when(employeeService.getById(employee2.getUuid())).thenReturn(employee2);
//        when(attendanceDao.getByEmployeeUuid(employee1.getUuid())).thenReturn(attendanceList1);
//        when(attendanceDao.getByEmployeeUuid(employee2.getUuid())).thenReturn(attendanceList2);
//
//        List<ViewEmployeeAttendanceDto> response = attendanceService.getAllEmployeesAttendanceByManager(managerUuid);
//
//        assertNotNull(response);
//        assertEquals(2, response.size());
//
//        assertEquals(employee1.getUuid(), response.get(0).getEmployeeUuid());
//        assertEquals(employee2.getUuid(), response.get(1).getEmployeeUuid());
//
//        verify(employeeService, times(1)).getByManagerUuid(managerUuid);
//        verify(employeeService, times(1)).getById(employee1.getUuid());
//        verify(employeeService, times(1)).getById(employee2.getUuid());
//        verify(attendanceDao, times(1)).getByEmployeeUuid(employee1.getUuid());
//        verify(attendanceDao, times(1)).getByEmployeeUuid(employee2.getUuid());
//    }
//
//    @Test
//    void getFullTeamAttendance_success() {
//        UUID employee1Uuid = UUID.randomUUID();
//        UUID employee2Uuid = UUID.randomUUID();
//        EmployeeAndManagerDto employee1Dto = EmployeeAndManagerDto.builder()
//                .uuid(employee1Uuid)
//                .firstName("test1")
//                .lastName("test1")
//                .build();
//        EmployeeAndManagerDto employee2Dto = EmployeeAndManagerDto.builder()
//                .uuid(employee2Uuid)
//                .firstName("test2")
//                .lastName("test2")
//                .build();
//        Employee employee1 = Employee.builder()
//                .uuid(employee1Uuid)
//                .firstName("test1")
//                .lastName("test1")
//                .build();
//        Employee employee2 = Employee.builder()
//                .uuid(employee2Uuid)
//                .firstName("test2")
//                .lastName("test2")
//                .build();
//
//        List<Attendance> attendanceList1 = List.of(
//                new Attendance(UUID.randomUUID(), employee1.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee1.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.HALF_DAY, AttendanceStatus.WAITING_FOR_CANCELLATION, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee1.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.CANCELLED, Date.from(Instant.now()), Instant.now(), Instant.now()));
//
//        List<Attendance> attendanceList2 = List.of(
//                new Attendance(UUID.randomUUID(), employee2.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee2.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.HALF_DAY, AttendanceStatus.WAITING_FOR_CANCELLATION, Date.from(Instant.now()), Instant.now(), Instant.now()),
//                new Attendance(UUID.randomUUID(), employee2.getUuid(), WorkMode.WORK_FROM_HOME, AttendanceType.FULL_DAY, AttendanceStatus.CANCELLED, Date.from(Instant.now()), Instant.now(), Instant.now()));
//        List<EmployeeAndManagerDto> fullTeam = List.of(employee1Dto, employee2Dto);
//        when(employeeService.getFullTeam(EMPLOYEE_UUID)).thenReturn(fullTeam);
//        when(employeeService.getById(employee1Uuid)).thenReturn(employee1);
//        when(employeeService.getById(employee2Uuid)).thenReturn(employee2);
//        when(attendanceDao.getByEmployeeUuid(employee1.getUuid())).thenReturn(attendanceList1);
//        when(attendanceDao.getByEmployeeUuid(employee2.getUuid())).thenReturn(attendanceList2);
//
//        List<ViewEmployeeAttendanceDto> response = attendanceService.getFullTeamAttendance(EMPLOYEE_UUID);
//        assertNotNull(response);
//        assertEquals(2, response.size());
//
//        verify(employeeService, times(1)).getFullTeam(EMPLOYEE_UUID);
//        verify(employeeService, times(1)).getById(employee1.getUuid());
//        verify(employeeService, times(1)).getById(employee2.getUuid());
//        verify(attendanceDao, times(1)).getByEmployeeUuid(employee1.getUuid());
//        verify(attendanceDao, times(1)).getByEmployeeUuid(employee2.getUuid());
//    }

}