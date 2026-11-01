package com.learning.emsmybatisliquibase.utils;

import com.learning.emsmybatisliquibase.entity.enums.DeviceType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UtilityService {
    private UtilityService() {
        /* This utility class should not be instantiated */
    }


    public static final String EMPLOYEE_UUID = "employeeUuid";

    public static final String PERIOD_UUID = "periodUuid";

    public static final String UUID_NAME = "uuid";

    public static final String STATUSES = "statuses";

    public static final String STATUS = "status";

    public static DeviceType getDeviceDetails(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return DeviceType.UNKNOWN;
        ua = ua.toLowerCase();

        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return DeviceType.TABLET;
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.WEB;
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (var cookie : cookies) {
                log.info("Cookie name: {} Cookie Value: {}", cookie.getName(), cookie.getValue());
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
