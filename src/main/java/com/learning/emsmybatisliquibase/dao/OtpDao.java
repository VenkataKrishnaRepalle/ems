package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.OtpAuth;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface OtpDao {

    List<OtpAuth> get(@Param("requestQuery") RequestQuery requestQuery);

    int insert(@Param("otp") OtpAuth otp);

    int update(@Param("otp") OtpAuth otp);

    int delete(@Param("uuid") UUID uuid);
}
