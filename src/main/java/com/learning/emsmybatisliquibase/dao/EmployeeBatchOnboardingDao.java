package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.EmployeeBatchOnboarding;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface EmployeeBatchOnboardingDao {

    int insert(@Param("ebo") EmployeeBatchOnboarding employeeBatchOnboarding);

    List<EmployeeBatchOnboarding> get(@Param("rq") RequestQuery requestQuery);

    Long getCount(@Param("rq") RequestQuery requestQuery);

    int updateStatus(@Param("uuid") UUID id, @Param("status") String status);
}
