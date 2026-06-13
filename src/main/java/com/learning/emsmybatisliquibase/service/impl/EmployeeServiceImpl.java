package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.EmployeePeriodDao;
import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.entity.enums.RoleType;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.EmployeeMapper;
import com.learning.emsmybatisliquibase.service.*;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_ALREADY_EXISTS;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_INTEGRATE_VIOLATION;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_NOT_CREATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_NOT_FOUND;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_NOT_UPDATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.MANAGER_ACCESS_NOT_FOUND;
import static com.learning.emsmybatisliquibase.utils.UtilityService.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeDao employeeDao;

    private final EmployeeMapper employeeMapper;

    private final ProfileService profileService;

    private final EmployeePeriodService employeePeriodService;

    private final PeriodService periodService;

    private final EmployeePeriodDao employeePeriodDao;

    private final Random random = new Random();

    private final EmployeeRoleService employeeRoleService;

    private final KeycloakService keycloakService;

    private final CamundaClient camundaClient;

    @Override
    @Transactional
    public ApiResponse<?> add(AddEmployeeDto employeeDto) {
        var validate = employeeDto.validate();
        if (!validate.isEmpty()) {
            throw new InvalidInputException("INVALID_EMPLOYEE_INPUT", validate);
        }
        var employee = employeeMapper.addEmployeeDtoToEmployee(employeeDto);
        if (employeeDto.getManagerUuid() != null && Boolean.FALSE.equals(isManager(employeeDto.getManagerUuid()))) {
            employee.setManagerUuid(null);
        }
        var employeeByEmail = employeeDao.getByEmail(employee.getEmail());
        if (employeeByEmail != null) {
            throw new FoundException(EMPLOYEE_ALREADY_EXISTS.code(), "Employee with given email already exists");
        }

        boolean isManager = "T".equalsIgnoreCase(employeeDto.getIsManager().trim()) ||
                "true".equalsIgnoreCase(employeeDto.getIsManager().trim());
        employee.setIsManager(isManager);
        employee.setManagerUuid(employee.getManagerUuid());
        if (null == employee.getUsername()) {
            employee.setUsername(employee.getEmail());
        }

        String password = validatePasswords(employeeDto.getPassword(), employeeDto.getConfirmPassword()) ?
                employeeDto.getPassword() : generateRandomPassword();
        try {
            camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId("onboarding_colleague")
                    .latestVersion()
                    .variables(Map.of("employeeDto", employeeDto, "employee", employee, "password", password))
                    .send();

        } catch (Exception e) {
            throw new IntegrityException("ONBOARDING_FAILED", e.getMessage());
        }
        return ApiResponse.success("Employee onboarded successfully");
    }

    @Override
    public Employee insert(Employee employee) {
        try {
            if (0 == employeeDao.insert(employee)) {
                throw new NotFoundException(EMPLOYEE_NOT_CREATED.code(), "Failed in saving employee");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_NOT_CREATED.code(), exception.getCause().getMessage());
        }
        return employee;
    }

    private boolean validatePasswords(String password, String confirmPassword) {
        return StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(confirmPassword);
    }


    @Override
    public Employee getById(UUID id) {
        var employee = employeeDao.get(id);
        if (employee == null) {
            throw new NotFoundException(EMPLOYEE_NOT_FOUND.code(), "employee not found with id: " + id);
        }
        return employee;
    }

    @Override
    public void updateLeavingDate(UUID id, UpdateLeavingDateDto updateLeavingDate) {
        var employee = getById(id);
        try {
            if (0 == employeeDao.updateLeavingDate(updateLeavingDate.getLeavingDate(), id)) {
                throw new InvalidInputException(EMPLOYEE_INTEGRATE_VIOLATION.code(),
                        "Error in updating LeavingDate");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_INTEGRATE_VIOLATION.code(),
                    exception.getCause().getMessage());
        }

        var profile = profileService.getByEmployeeUuid(id);

        if (updateLeavingDate.getLeavingDate() == null && profile.getProfileStatus().equals(ProfileStatus.INACTIVE)) {
            var currentActiveCycle = periodService.getCurrentActivePeriod();
            var employeeCycle = employeePeriodDao.get(new RequestQuery(
                    Map.of(EMPLOYEE_UUID, employee.getUuid(), PERIOD_UUID, currentActiveCycle.getUuid())
            ));
            if (employeeCycle != null) {
                employeePeriodService.updateEmployeePeriodStatus(employeeCycle.getFirst().getUuid(),
                        PeriodStatus.STARTED);
            }
            profile.setProfileStatus(ProfileStatus.ACTIVE);
        } else if (updateLeavingDate.getLeavingDate() != null &&
                updateLeavingDate.getLeavingDate().before(new Date())) {
            profile.setProfileStatus(ProfileStatus.INACTIVE);
            var empStartedCycles = employeePeriodDao.get(new RequestQuery(
                    Map.of(EMPLOYEE_UUID, employee.getUuid(), STATUS, PeriodStatus.STARTED)
            ));
            empStartedCycles.forEach(employeeCycle ->
                    employeePeriodService.updateEmployeePeriodStatus(employeeCycle.getUuid(),
                            PeriodStatus.INACTIVE));
        }
        profileService.update(profile);

        updateKeycloak(employee, false);
    }


    @Override
    public List<Employee> getAll() {
        return employeeDao.getAll();
    }

    public void update(Employee employee) {
        updateToDB(employee);

        updateKeycloak(employee, true);
    }

    private void updateToDB(Employee employee) {
        try {
            if (0 == employeeDao.update(employee)) {
                throw new NotFoundException(EMPLOYEE_NOT_UPDATED.code(), "Failed in updating employee manager status");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_NOT_UPDATED.code(), exception.getCause().getMessage());
        }
    }

    private void updateKeycloak(Employee employee, boolean enabled) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(employee.getUsername());
        userRepresentation.setEmail(employee.getEmail());
        userRepresentation.setFirstName(employee.getFirstName());
        userRepresentation.setLastName(employee.getLastName());
        userRepresentation.setEmail(employee.getEmail());
        userRepresentation.setEnabled(enabled);

        keycloakService.update(userRepresentation);
    }

    public Employee getByEmail(String email) {
        var employee = employeeDao.getByEmail(email.trim());
        if (employee == null) {
            throw new NotFoundException(EMPLOYEE_NOT_FOUND.code(), "Employee not found with email " + email);
        }
        return employee;
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return Optional.ofNullable(employeeDao.getByEmail(email));
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        return Optional.ofNullable(employeeDao.getByUsername(username));
    }

    @Override
    public List<EmployeeDetailsDto> getByManagerUuid(UUID managerId) {
        var employee = getById(managerId);
        if (employee.getIsManager().equals(Boolean.TRUE)) {
            return getAllByManagerUuid(managerId);
        } else {
            throw new NotFoundException(MANAGER_ACCESS_NOT_FOUND.code(), "User don't have manager access");
        }
    }

    private List<EmployeeDetailsDto> getAllByManagerUuid(UUID mangerUuid) {
        return employeeDao.getEmployeesByManager(mangerUuid);
    }

    public Boolean isManager(UUID uuid) {
        var manager = employeeDao.get(uuid);
        return manager != null && manager.getIsManager();
    }

    @Override
    public HashMap<String, List<EmployeeResponseDto>> getFullTeam(UUID employeeId) {
        var me = getById(employeeId);
        if (Boolean.FALSE.equals(me.getIsManager())) {
            throw new NotFoundException(MANAGER_ACCESS_NOT_FOUND.code(), "User don't have manager access");
        }
        HashMap<String, List<EmployeeResponseDto>> fullTeam = new HashMap<>();

        if (me.getManagerUuid() != null) {
            fullTeam.put("myManagerReportees", employeeDao.getEmployeeFullDetailsByManager(me.getManagerUuid()));

        }
        fullTeam.put("myReportees", employeeDao.getEmployeeFullDetailsByManager(employeeId));
        return fullTeam;
    }

    @Override
    public EmployeeFullReportingChainDto getEmployeeFullReportingChain(UUID employeeId) {
        var employee = getById(employeeId);
        var response = employeeMapper.employeeResponseDtoToEmployeeFullReportingChainDto(employee);
        if (employee.getManagerUuid() != null) {
            response.setManager(getEmployeeFullReportingChain(employee.getManagerUuid()));
        }
        return response;
    }

    @Override
    public EmployeeResponseDto getMe() {
        var employeeUuid = getAuthenticatedUser();
        if (employeeUuid == null) {
            throw new InvalidInputException("AUTHENTICATION_FAILED", "Authentication failed for user. Please login again.");
        }
        var employee = employeeDao.getEmployee(employeeUuid);
        var roles = employeeRoleService.getRolesByEmployeeUuid(employee.getUuid())
                .stream()
                .map(RoleType::toString)
                .toList();
        if (!roles.isEmpty()) {
            employee.setRoles(roles);
        }
        return employee;
    }

    @Override
    public UUID getAuthenticatedUser() {
        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getName() != null) {
            return UUID.fromString(authentication.getName());
        }

        return null;
    }

    @Override
    public PaginatedResponse<Employee> getAllByPagination(int page, int size, String sortBy, String sortOrder, List<ProfileStatus> profileStatuses) {
        page = Math.max(page, 1);
        size = Math.max(size, 1);
        int offSet = (page - 1) * size;
        var totalCount = employeeDao.employeesCount(profileStatuses);
        var employees = employeeDao.findAll(size, offSet, setSortBy(sortBy), sortOrder, profileStatuses);
        return PaginatedResponse.<Employee>builder()
                .data(totalCount > 0 ? employees.getEmployees() : Collections.emptyList())
                .totalItems(totalCount > 0 ? totalCount : 0)
                .totalPages(totalCount > 0 ? totalCount / size : 0)
                .currentPage(page)
                .build();
    }

    private String setSortBy(String sortBy) {
        if (null == sortBy) {
            return "uuid";
        } else if (sortBy.equalsIgnoreCase("firstName")) {
            return "first_name";
        } else if (sortBy.equalsIgnoreCase("lastName")) {
            return "last_name";
        } else if (sortBy.equalsIgnoreCase("email")) {
            return "email";
        } else if (sortBy.equalsIgnoreCase("dateOfBirth")) {
            return "date_of_birth";
        } else if (sortBy.equalsIgnoreCase("phoneNumber")) {
            return "phone_number";
        } else if (sortBy.equalsIgnoreCase("createdTime")) {
            return "created_time";
        } else if (sortBy.equalsIgnoreCase("updatedTime")) {
            return "updated_time";
        } else if (sortBy.equalsIgnoreCase("joiningDate")) {
            return "joining_date";
        } else {
            return "uuid";
        }
    }

    @Override
    public List<EmployeeDetailsDto> getAllActiveManagers() {
        return employeeDao.getAllActiveManagers();
    }

    @Override
    public List<EmployeeDetailsDto> getByNameOrEmail(String name) {
        return employeeDao.getByNameOrEmail(name);
    }

    @Override
    public List<EmployeeEtmsDetails> getAllForEtms() {
        return employeeDao.getAllForEtms();
    }

    @Override
    public void delete(UUID uuid) {
        try {
            if (0 == employeeDao.delete(uuid)) {
                throw new IntegrityException("EMPLOYEE_DELETE_FAILED", "Employee delete failed for employee: " + uuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("EMPLOYEE_DELETE_FAILED", exception.getCause().getMessage());
        }
    }

    private String generateRandomPassword() {
        var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        var sb = new StringBuilder(10);

        for (int i = 0; i < sb.capacity(); i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }
}