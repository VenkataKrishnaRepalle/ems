package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.service.AzureBlobService;
import com.learning.emsmybatisliquibase.service.SparkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
public class SparkServiceImpl implements SparkService {

    @Value("${spring.datasource.url}")
    String dbUrl;

    @Value("${azure.storage.container-name}")
    String containerName;

    private final Properties properties;
    private final SparkSession spark;
    private final AzureBlobService azureBlobService;

    public SparkServiceImpl(@Qualifier("dbProperties") Properties properties, SparkSession spark, AzureBlobService azureBlobService) {
        this.properties = properties;
        this.spark = spark;
        this.azureBlobService = azureBlobService;
    }

    @Override
    public void processAndStoreData() {

        Dataset<Row> employees = spark.read()
                .jdbc(dbUrl, "employee", properties);

        employees.show();

        String localDir = "/tmp/employees";

        employees.coalesce(1)
                .write()
                .mode("overwrite")
                .option("header", "true")
                .csv(localDir);

        log.info("Spark wrote CSV locally at {}", localDir);

        // Find CSV file
        File dir = new File(localDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.error("No CSV written by Spark!");
            return;
        }
        File csv = files[0];

        // Upload to blob
        String blobPath = "employees/report.csv";
        azureBlobService.uploadFile(containerName, blobPath, csv.getAbsolutePath());

        log.info("Uploaded CSV to blob: {}", blobPath);
    }


    public void readAndProcessData() {

        String downloadPath = "/tmp/employees_download/report.csv";

        azureBlobService.downloadFile(containerName, "employees/report.csv", downloadPath);

        Dataset<Row> df = spark.read()
                .option("header", "true")
                .csv(downloadPath);

        df.show();
    }


    private String getAzurePath(String path) {
        return String.format("wasb://%s@devstoreaccount1.blob.core.windows.net/%s", containerName, path);
    }

}
