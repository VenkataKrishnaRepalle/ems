package com.learning.emsmybatisliquibase.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Component
@MappedJdbcTypes(JdbcType.OTHER)
@RequiredArgsConstructor
public class ListTypeHandler extends BaseTypeHandler<List<AddEmployeeDto>> {

    private final ObjectMapper mapper;

    @Override
    public void setNonNullParameter(
            PreparedStatement ps,
            int i,
            List<AddEmployeeDto> parameter,
            JdbcType jdbcType) throws SQLException {

        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");

        try {
            jsonObject.setValue(
                    mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException(e);
        }

        ps.setObject(i, jsonObject);
    }

    @Override
    public List<AddEmployeeDto> getNullableResult(ResultSet rs, String columnName)
            throws SQLException {

        return convert(rs.getString(columnName));
    }

    @Override
    public List<AddEmployeeDto> getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {

        return convert(rs.getString(columnIndex));
    }

    @Override
    public List<AddEmployeeDto> getNullableResult(
            CallableStatement cs,
            int columnIndex)
            throws SQLException {

        return convert(cs.getString(columnIndex));
    }

    private List<AddEmployeeDto> convert(String json)
            throws SQLException {

        if (json == null) {
            return Collections.emptyList();
        }

        try {
            return mapper.readValue(
                    json,
                    new TypeReference<List<AddEmployeeDto>>() {});
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}