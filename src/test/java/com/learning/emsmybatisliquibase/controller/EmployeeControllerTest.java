package com.learning.emsmybatisliquibase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {
    MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    ObjectMapper objectMapper = new ObjectMapper();

    private Employee employee;
    private EmployeeResponseDto employeeResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();

        employee = Employee.builder()
                .uuid(UUID.randomUUID())
                .firstName("testFirstName")
                .lastName("testLastName")
                .email("test@gmail.com")
                .managerUuid(UUID.randomUUID())
                .build();

        employeeResponseDto = EmployeeResponseDto.builder()
                .uuid(employee.getUuid())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .managerUuid(employee.getManagerUuid())
                .build();
    }

    @Test
    void testAdd() throws Exception {
        AddEmployeeDto addEmployeeDto = AddEmployeeDto.builder()
                .email("test@email.com")
                .firstName("test1")
                .lastName("test2")
                .gender(Gender.MALE)
                .build();
        AddEmployeeResponseDto addEmployeeResponseDto = AddEmployeeResponseDto.builder()
                .email("test@email.com")
                .firstName("test1")
                .lastName("test2")
                .gender(Gender.MALE)
                .build();
        when(employeeService.add(addEmployeeDto)).thenReturn(addEmployeeResponseDto);

        mockMvc.perform(post("/api/employee/add")
                        .content(objectMapper.writeValueAsString(addEmployeeDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(addEmployeeResponseDto.getEmail()))
                .andExpect(jsonPath("$.firstName").value(addEmployeeResponseDto.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(addEmployeeResponseDto.getLastName()))
                .andExpect(jsonPath("$.gender").value(addEmployeeResponseDto.getGender().toString()));
    }

    @Test
    void testViewEmployee() throws Exception {
        when(employeeService.getById(any(UUID.class))).thenReturn(employee);

        mockMvc.perform(get("/api/employee/getById/{id}", employee.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(employee.getUuid().toString()))
                .andExpect(jsonPath("$.email").value(employee.getEmail()))
                .andExpect(jsonPath("$.firstName").value(employee.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(employee.getLastName()));
    }

    @Test
    void testViewAll() throws Exception {
        when(employeeService.getAll()).thenReturn(List.of(employee));

        mockMvc.perform(get("/api/employee/getAll")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(employee.getUuid().toString()))
                .andExpect(jsonPath("$[0].email").value(employee.getEmail()))
                .andExpect(jsonPath("$[0].firstName").value(employee.getFirstName()))
                .andExpect(jsonPath("$[0].lastName").value(employee.getLastName()));
    }

    @Test
    void testUpdateLeavingDate() throws Exception {
        UpdateLeavingDateDto leavingDateDto = UpdateLeavingDateDto.builder()
                .leavingDate(new Date())
                .build();
        doNothing().when(employeeService).updateLeavingDate(employee.getUuid(), leavingDateDto);

        mockMvc.perform(post("/api/employee/updateLeavingDate/{id}", employee.getUuid())
                        .content(objectMapper.writeValueAsString(leavingDateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

//    @Test
//    void testManagerAccess() throws Exception {
//        FileInputStream file = new FileInputStream("src/main/resources/files/ManagerPermission.xlsx");
//        MultipartFile multipartFile = new MockMultipartFile("file", "file.xlsx", MediaType.MULTIPART_FORM_DATA_VALUE, file);
//
//        doNothing().when(employeeService).managerAccess(multipartFile);
//
//        mockMvc.perform(multipart("/api/employee/managerAccess/file")
//                        .file((MockMultipartFile) multipartFile))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testUpdateManagerId() throws Exception {
//        FileInputStream file = new FileInputStream("src/main/resources/files/AddManagerIdToEmployees.xlsx");
//        MultipartFile multipartFile = new MockMultipartFile("file", "file.xlsx", MediaType.MULTIPART_FORM_DATA_VALUE, file);
//
//        doNothing().when(employeeService).updateManagerId(multipartFile);
//
//        mockMvc.perform(multipart("/api/employee/updateManagerId/file")
//                        .file((MockMultipartFile) multipartFile))
//                .andExpect(status().isOk());
//    }

    @Test
    void testGetByManagerId() throws Exception {
        when(employeeService.getByManagerUuid(any(UUID.class))).thenReturn(List.of(employeeResponseDto));

        mockMvc.perform(get("/api/employee/getByManagerId/{id}", employee.getManagerUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(employee.getUuid().toString()))
                .andExpect(jsonPath("$[0].email").value(employee.getEmail()))
                .andExpect(jsonPath("$[0].firstName").value(employee.getFirstName()))
                .andExpect(jsonPath("$[0].lastName").value(employee.getLastName()));
    }

//    @Test
//    void testGetFullTeam() throws Exception {
//        EmployeeAndManagerDto employeeAndManagerDto = EmployeeAndManagerDto.builder()
//                .uuid(employee.getUuid())
//                .firstName(employee.getFirstName())
//                .lastName(employee.getLastName())
//                .email(employee.getEmail())
//                .managerUuid(employee.getManagerUuid())
//                .manager(EmployeeResponseDto.builder()
//                        .uuid(employee.getManagerUuid())
//                        .build())
//                .build();
//        ConcurrentHashMap map = new ConcurrentHashMap<>();
//        map.put("my-full-reporting", List.of(employeeResponseDto));
//        when(employeeService.getFullTeam(any(UUID.class))).thenReturn(map);
//
//        mockMvc.perform(get("/api/employee/getFullTeam/{id}", employeeAndManagerDto.getUuid())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].uuid").value(employeeAndManagerDto.getUuid().toString()))
//                .andExpect(jsonPath("$[0].email").value(employeeAndManagerDto.getEmail()))
//                .andExpect(jsonPath("$[0].firstName").value(employeeAndManagerDto.getFirstName()))
//                .andExpect(jsonPath("$[0].lastName").value(employeeAndManagerDto.getLastName()))
//                .andExpect(jsonPath("$[0].managerUuid").value(employeeAndManagerDto.getManagerUuid().toString()))
//                .andExpect(jsonPath("$[0].manager.uuid").value(employeeAndManagerDto.getManager().getUuid().toString()));
//    }

    @Test
    void testEmployeeFullReportingChain() throws Exception {
        UUID employeeUuid = UUID.randomUUID(), managerUuid = UUID.randomUUID(), managerMangerUuid = UUID.randomUUID();
        EmployeeFullReportingChainDto employeeFullReportingChainDto = EmployeeFullReportingChainDto.builder()
                .uuid(employeeUuid)
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .manager(EmployeeFullReportingChainDto.builder()
                        .uuid(managerUuid)
                        .manager(EmployeeFullReportingChainDto.builder()
                                .uuid(managerMangerUuid)
                                .build())
                        .build())
                .build();
        when(employeeService.getEmployeeFullReportingChain(employeeUuid)).thenReturn(employeeFullReportingChainDto);

        mockMvc.perform(get("/api/employee/getEmployeeFullReportingChain/{id}", employeeUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(employeeUuid.toString()))
                .andExpect(jsonPath("$.manager.uuid").value(managerUuid.toString()))
                .andExpect(jsonPath("$.manager.manager.uuid").value(managerMangerUuid.toString()));
    }

//    @Test
//    void testColleagueOnBoard() throws Exception {
//        FileInputStream file = new FileInputStream("src/main/resources/files/Employee on-board.xlsx");
//        MultipartFile multipartFile = new MockMultipartFile("file", "file.xlsx", MediaType.MULTIPART_FORM_DATA_VALUE, file);
//
//        SuccessResponseDto successResponseDto = SuccessResponseDto.builder()
//                .success(true)
//                .build();
//        when(employeeService.colleagueOnboard(multipartFile)).thenReturn(successResponseDto);
//
//        mockMvc.perform(multipart("/api/employee/employee-onboard/file")
//                        .file((MockMultipartFile) multipartFile))
//                .andExpect(status().isOk());
//    }
}
