package com.learning.emsmybatisliquibase.mapper;

import com.learning.emsmybatisliquibase.entity.Salary;
import com.learning.emsmybatisliquibase.entity.audit.SalaryAudit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SalaryMapper {

    SalaryAudit salaryToSalaryAudit(Salary salary);
}
