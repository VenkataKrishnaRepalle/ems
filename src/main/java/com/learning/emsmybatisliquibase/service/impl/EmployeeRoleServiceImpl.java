package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.EmployeeRoleDao;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.EmployeeRole;
import com.learning.emsmybatisliquibase.entity.enums.RoleType;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.EmployeeRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_NOT_FOUND;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeRoleErrorCodes.EMPLOYEE_ROLE_ALREADY_EXISTS;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeRoleErrorCodes.EMPLOYEE_ROLE_NOT_CREATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeRoleErrorCodes.EMPLOYEE_ROLE_NOT_DELETED;

@Service
@RequiredArgsConstructor
public class EmployeeRoleServiceImpl implements EmployeeRoleService {

    private final EmployeeRoleDao employeeRoleDao;

    private final EmployeeDao employeeDao;

    @Override
    public EmployeeRole add(EmployeeRole employeeRole) {
        getEmployeeById(employeeRole.getEmployeeUuid());

        var employeeRoleList = employeeRoleDao.getByEmployeeUuid(employeeRole.getEmployeeUuid());

        employeeRoleList.forEach(employeeRole1 -> {
            if (employeeRole1.getRole().equals(employeeRole.getRole())) {
                throw new InvalidInputException(EMPLOYEE_ROLE_ALREADY_EXISTS.code(), "Employee already has this role");
            }
        });
        try {
            if (0 == employeeRoleDao.insert(employeeRole)) {
                throw new IntegrityException(EMPLOYEE_ROLE_NOT_CREATED.code(), "Insert failed");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_ROLE_ALREADY_EXISTS.code(), "Employee already has this role");
        }

        return employeeRole;
    }

    @Override
    public void delete(EmployeeRole employeeRole) {
        getEmployeeById(employeeRole.getEmployeeUuid());
        try {
            if (0 == employeeRoleDao.delete(employeeRole.getEmployeeUuid(), employeeRole.getRole().toString())) {
                throw new InvalidInputException(EMPLOYEE_ROLE_NOT_DELETED.code(), "Delete failed");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_ROLE_NOT_DELETED.code(), "Employee already has this role");
        }
    }

    @Override
    public List<RoleType> getRolesByEmployeeUuid(UUID employeeUuid) {
        var employee = getEmployeeById(employeeUuid);
        List<RoleType> roles = new ArrayList<>();
        roles.add(RoleType.EMPLOYEE);
        if (Boolean.TRUE.equals(employee.getIsManager())) roles.add(RoleType.MANAGER);

        var employeeExtraRoles = employeeRoleDao.getByEmployeeUuid(employeeUuid);
        employeeExtraRoles.forEach(employeeRole -> roles.add(employeeRole.getRole()));
        return roles;
    }

    private Employee getEmployeeById(UUID employeeUuid) {
        var employee = employeeDao.get(employeeUuid);
        if (employee == null) {
            throw new NotFoundException(EMPLOYEE_NOT_FOUND.code(), "employee not found with id: " + employeeUuid);
        }
        return employee;
    }
}
