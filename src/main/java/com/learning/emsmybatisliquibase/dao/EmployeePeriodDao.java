package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.EmployeePeriod;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface EmployeePeriodDao {

    int insert(@Param("employeePeriod") EmployeePeriod employeePeriod);

    int update(@Param("employeePeriod") EmployeePeriod employeePeriod);

    List<EmployeePeriod> get(@Param("request") RequestQuery requestQuery);
}
