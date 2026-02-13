package com.learning.emsmybatisliquibase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dto.AttendanceDto;
import com.learning.emsmybatisliquibase.dto.ViewEmployeeAttendanceDto;
import com.learning.emsmybatisliquibase.entity.Attendance;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceType;
import com.learning.emsmybatisliquibase.entity.enums.WorkMode;
import com.learning.emsmybatisliquibase.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    MockMvc mockMvc;

    @InjectMocks
    AttendanceController attendanceController;

    @Mock
    AttendanceService attendanceService;

    private static final UUID EMPLOYEE_UUID = UUID.randomUUID();

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController).build();
    }


    @Test
    void testApply() throws Exception {
        List<AttendanceDto> attendanceDtos = List.of(
                new AttendanceDto(WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now()),
                new AttendanceDto(WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now())
        );

        List<Attendance> attendances = List.of(
                new Attendance(UUID.randomUUID(), EMPLOYEE_UUID, WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now()),
                new Attendance(UUID.randomUUID(), EMPLOYEE_UUID, WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now())
        );

        when(attendanceService.apply(EMPLOYEE_UUID, attendanceDtos)).thenReturn(attendances);

        mockMvc.perform(post("/api/attendance/apply/{employeeUuid}", EMPLOYEE_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendanceDtos))
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].employeeUuid").value(EMPLOYEE_UUID.toString()));

        verify(attendanceService, times(1)).apply(EMPLOYEE_UUID, attendanceDtos);
    }

    @Test
    void testUpdate() throws Exception {
        UUID attendanceUuid = UUID.randomUUID();
        AttendanceDto attendanceDto = new AttendanceDto(WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now());

        Attendance attendance = new Attendance(attendanceUuid, EMPLOYEE_UUID, WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now());

        when(attendanceService.update(EMPLOYEE_UUID, attendanceUuid, attendanceDto)).thenReturn(attendance);

        mockMvc.perform(put("/api/attendance/update/{employeeUuid}/attendance/{attendanceUuid}", EMPLOYEE_UUID, attendanceUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendanceDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.employeeUuid").value(EMPLOYEE_UUID.toString()))
                .andExpect(jsonPath("$.uuid").value(attendanceUuid.toString()));
    }

    @Test
    void testGet() throws Exception {
        UUID attendanceUuid = UUID.randomUUID();

        Attendance attendance = new Attendance(attendanceUuid, EMPLOYEE_UUID, WorkMode.WORK_FROM_OFFICE, AttendanceType.FULL_DAY, AttendanceStatus.SUBMITTED, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now());

        when(attendanceService.getByUuid(EMPLOYEE_UUID, attendanceUuid)).thenReturn(attendance);

        mockMvc.perform(get("/api/attendance/get/{employeeUuid}/attendance/{attendanceUuid}", EMPLOYEE_UUID, attendanceUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeUuid").value(EMPLOYEE_UUID.toString()))
                .andExpect(jsonPath("$.uuid").value(attendanceUuid.toString()));

        verify(attendanceService, times(1)).getByUuid(EMPLOYEE_UUID, attendanceUuid);
    }
    @Test

    void testGetEmployeeAttendance() throws Exception {
        ViewEmployeeAttendanceDto employeeAttendanceDto = new ViewEmployeeAttendanceDto(EMPLOYEE_UUID, "test first name", "test last name", List.of(), List.of(), List.of());

        when(attendanceService.getEmployeeAttendance(EMPLOYEE_UUID, null, null)).thenReturn(employeeAttendanceDto);

        mockMvc.perform(get("/api/attendance/get/{employeeUuid}", EMPLOYEE_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeUuid").value(EMPLOYEE_UUID.toString()))
                .andExpect(jsonPath("$.employeeFirstName").value(employeeAttendanceDto.getEmployeeFirstName()))
                .andExpect(jsonPath("$.employeeLastName").value(employeeAttendanceDto.getEmployeeLastName()));

        verify(attendanceService, times(1)).getEmployeeAttendance(EMPLOYEE_UUID, null, null);
    }

    @Test
    void testGetFullTeamAttendance() throws Exception {
        List<ViewEmployeeAttendanceDto> employeeAttendanceDtos = List.of(
                new ViewEmployeeAttendanceDto(EMPLOYEE_UUID, "test first name", "test last name", List.of(), List.of(), List.of()),
                new ViewEmployeeAttendanceDto(UUID.randomUUID(), "test first name", "test last name", List.of(), List.of(), List.of())
        );

        when(attendanceService.getTeamAttendance(EMPLOYEE_UUID, 2000L, null)).thenReturn(employeeAttendanceDtos);

        mockMvc.perform(get("/api/attendance/get/attendance/full-team/{employeeUuid}", EMPLOYEE_UUID, 2000L)
                       .contentType(MediaType.APPLICATION_JSON)
                       .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(attendanceService, times(1)).getTeamAttendance(EMPLOYEE_UUID, 2000L, null);
    }

}