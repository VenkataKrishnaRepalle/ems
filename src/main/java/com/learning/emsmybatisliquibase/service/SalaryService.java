package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.entity.Salary;

import java.util.Map;
import java.util.UUID;

public interface SalaryService {

    Salary process(String type, String salary, String joiningBonus);

    Salary insert(Salary salary);

    Map<String, Object> get(UUID employeeUuid);

    Salary update(UUID employeeUuid, String percentage);
}
