package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.entity.Salary;
import com.learning.emsmybatisliquibase.entity.audit.SalaryAudit;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface SalaryDao {
    int insert(@Param("salary") Salary salary);

    Salary get(@Param("employeeUuid")UUID employeeUuid);

    int insertAudit(@Param("salaryAudit") SalaryAudit salaryAudit);

    List<SalaryAudit> getAudit(@Param("employeeUuid")UUID employeeUuid);

    int update(@Param("salary") Salary salary);
}
