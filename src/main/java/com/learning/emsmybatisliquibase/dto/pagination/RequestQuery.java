package com.learning.emsmybatisliquibase.dto.pagination;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@ToString
public class RequestQuery {
    private Map<String, Object> properties;

    @JsonAnySetter
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public void setProperties(Map<String, Object> values) {
        properties.putAll(values);
    }

    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }

    public Object getPropertyValue(String propertyName) {
        return properties.get(propertyName);
    }

    public String getPropertyAsString(String propertyName) {
        var value = properties.get(propertyName);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public List<?> getPropertyAsList(String propertyName) {
        var lists = properties.get(propertyName);
        if (lists instanceof List<?>) {
            return (List<?>) lists;
        }
        return null;
    }

    public Map<?, ?> getPropertyAsMap(String propertyName) {
        var map = properties.get(propertyName);
        if (map instanceof Map<?, ?>) {
            return (Map<?, ?>) map;
        }
        return null;
    }

    public RequestQuery() {
        properties = new ConcurrentHashMap<>();
    }

    public RequestQuery(Map<String, Object> properties) {
        this.properties = new ConcurrentHashMap<>(properties);
    }
}
