package com.learning.emsmybatisliquibase.service;


import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeService {

    AddEmployeeResponseDto add(AddEmployeeDto employeeDto) throws MessagingException, UnsupportedEncodingException;

    Employee getById(UUID id);

    Optional<Employee> findById(UUID id);

    Employee getByEmail(String email);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUsername(String username);

    void updateLeavingDate(UUID id, UpdateLeavingDateDto updateLeavingDate);

    List<Employee> getAll();

    void update(Employee employee);

    List<EmployeeResponseDto> getByManagerUuid(UUID managerId);

    Boolean isManager(UUID uuid);

    HashMap<String, List<EmployeeResponseDto>> getFullTeam(UUID employeeId);

    EmployeeFullReportingChainDto getEmployeeFullReportingChain(UUID employeeId);

    EmployeeResponseDto getMe();

    PaginatedResponse<Employee> getAllByPagination(int page, int size, String sortBy, String sortOrder, List<ProfileStatus> profileStatuses);

    List<EmployeeDetailsDto> getAllActiveManagers();

    List<EmployeeDetailsDto> getByNameOrEmail(String name);
}
