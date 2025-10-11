package com.learning.emsmybatisliquibase.service;


import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.entity.Employee;
import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeService {

    AddEmployeeResponseDto add(AddEmployeeDto employeeDto) throws MessagingException, UnsupportedEncodingException;

    Employee getById(UUID id);

    Employee getByEmail(String email);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUsername(String username);

    void updateLeavingDate(UUID id, UpdateLeavingDateDto updateLeavingDate);

    List<Employee> getAll();

    void update(Employee employee);

    List<Employee> getByManagerUuid(UUID managerId);

    void isManager(UUID uuid);

    List<EmployeeAndManagerDto> getFullTeam(UUID employeeId);

    EmployeeFullReportingChainDto getEmployeeFullReportingChain(UUID employeeId);

    EmployeeResponseDto getMe();

    PaginatedResponse<Employee> getAllByPagination(int page, int size, String sortBy, String sortOrder);

    List<EmployeeDetailsDto> getAllActiveManagers();

    List<EmployeeDetailsDto> getByNameOrEmail(String name);
}
