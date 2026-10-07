package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecution;
import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecutionStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ProcessExecutionDao {

    ProcessExecution getByProcessInstanceId(@Param("id") Long processInstanceId);

    List<ProcessExecution> getByEmployeeId(@Param("employeeUuid") UUID employeeUuid);

    int insert(@Param("pe") ProcessExecution pe);

    int update(@Param("pe") ProcessExecution pe);

    int updateEmployee(@Param("id") Long id, @Param("employeeUuid") UUID employeeUuid);

    int updateStatus(@Param("id") Long id, @Param("status") ProcessExecutionStatus status);

    int deleteByProcessId(@Param("id") Long processInstanceId);

    int deleteByEmployeeId(@Param("employeeUuid") UUID employeeUuid);
}
