package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public interface FilesService {

    ApiResponse<AtomicLong> colleagueOnboard(MultipartFile file) throws IOException;

    void managerAccess(MultipartFile file) throws IOException;

    void updateManagerId(MultipartFile file) throws IOException;

    void departmentPermission(MultipartFile file) throws IOException;
}
