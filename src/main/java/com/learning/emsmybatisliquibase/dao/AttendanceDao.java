package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.entity.Attendance;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface AttendanceDao {
    int insert(@Param("attendance") Attendance attendance);

    int update(@Param("attendance") Attendance attendance);

    Attendance getById(@Param("uuid") UUID uuid);

    List<Attendance> getByEmployeeUuid(@Param("employeeUuid") UUID uuid, @Param("year") Long year, @Param("month") Integer month);

    int delete(@Param("uuid") UUID uuid);
}
