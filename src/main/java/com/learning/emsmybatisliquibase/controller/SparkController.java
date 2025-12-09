package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.service.SparkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/spark")
@Slf4j
@RequiredArgsConstructor
public class SparkController {

    private final SparkService sparkService;

    private final SparkSession sparkSession;

    @GetMapping
    public ResponseEntity<Map<String, String>> testSpark() {
        Map<String, String> response = new HashMap<>();
        try {
            if (sparkSession == null) {
                log.info("SparkSession is not initialized");
            }
            
            // Simple test operation
            long count = Objects.requireNonNull(sparkSession).range(1, 10).count();
            response.put("status", "success");
            response.put("message", "Spark is working! Count: " + count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in testSpark", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/employees")
    public ResponseEntity<byte[]> printEmployees() throws IOException {
        byte[] csvData = sparkService.readData();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "employees-" + LocalDateTime.now() + ".csv");

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
}
