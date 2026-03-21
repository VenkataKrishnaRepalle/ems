package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FilesService {

    SuccessResponseDto colleagueOnboard(MultipartFile file) throws IOException;

    void managerAccess(MultipartFile file) throws IOException;

    void updateManagerId(MultipartFile file) throws IOException;

    void departmentPermission(MultipartFile file) throws IOException;
}
