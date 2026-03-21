package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.service.FilesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files/")
public class FilesController {

    private final FilesService filesService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "employee-onboard/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDto> colleagueOnboard(@RequestParam(name = "file") MultipartFile file)
            throws IOException {
        return new ResponseEntity<>(filesService.colleagueOnboard(file), HttpStatus.OK);
    }

    @PostMapping(value = "/managerAccess/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpStatus> managerAccess(@RequestParam(name = "file") MultipartFile file)
            throws IOException {
        filesService.managerAccess(file);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/updateManagerId/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpStatus> updateManagerId(@RequestParam(name = "file") MultipartFile file)
            throws IOException {
        filesService.updateManagerId(file);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/departmentPermission/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpStatus> departmentPermission(@RequestParam(name = "file") MultipartFile file)
            throws IOException {
        filesService.departmentPermission(file);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
