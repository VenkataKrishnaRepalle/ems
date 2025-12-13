package com.learning.emsmybatisliquibase.utils;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class UtilityService {

    public static final String LOCATION = "location";

    public static final String EMPLOYEE_UUID = "employeeUuid";

    public static final String PERIOD_UUID = "periodUuid";

    public static final String UUID_NAME = "uuid";

    public static final String STATUSES = "statuses";

    public static final String STATUS = "status";

    public static Map<String, String> getLocationInfo(RequestQuery requestQuery) {
        var location = requestQuery.getPropertyValue(LOCATION);
        if (location instanceof Map<?, ?>) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) location).entrySet()) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return result;
        }

        return Collections.emptyMap();
    }

    public static String getPlatform(RequestQuery requestQuery) {
        var platform = requestQuery.getPropertyValue("platform");
        if (platform instanceof String) {
            return (String) platform;
        }
        return null;
    }

    public static String extractAddressInfo(RequestQuery requestQuery) {
        var resultsObj = requestQuery.getPropertyValue("results");
        log.info(resultsObj.toString());
        if (!(resultsObj instanceof List<?> results)) {
            return null;
        }
        if (results.isEmpty()) {
            return null;
        }
        var result = results.getFirst();
        if (!(result instanceof Map<?, ?> resultMap)) {
            return null;
        }
        var componentsObj = resultMap.get("components");
        if (!(componentsObj instanceof Map<?, ?> components)) {
            return null;
        }
        var residential = extractValueFromMap(components, "residential");
        var road = extractValueFromMap(components, "road");
        var city = extractValueFromMap(components, "city");
        var district = extractValueFromMap(components, "district");
        var state = extractValueFromMap(components, "state");
        var postcode = extractValueFromMap(components, "postcode");
        var country = extractValueFromMap(components, "country");

        return Stream.of(residential, road, city, district, state, postcode, country)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    public static String extractValueFromMap(Map<?, ?> inputMap, String key) {
        if (!inputMap.containsKey(key)) {
            return null;
        }
        return inputMap.get(key).toString();
    }

}
