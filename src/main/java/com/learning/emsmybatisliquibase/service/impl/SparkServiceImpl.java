package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.service.SparkService;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Properties;

@Service
public class SparkServiceImpl implements SparkService {

    @Value("${spring.datasource.url}")
    String dbUrl;

    private final Properties properties;

    private final SparkSession spark;

    public SparkServiceImpl(@Qualifier("dbProperties") Properties properties, SparkSession spark) {
        this.properties = properties;
        this.spark = spark;
    }

    @Override
    public byte[] readData() throws IOException {
        Dataset<Row> employees = spark.read()
                .jdbc(dbUrl, "employee", properties);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {

            String[] columns = employees.columns();
            writer.println(String.join(",", columns));

            Iterator<Row> rowIterator = employees.toLocalIterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                for (int i = 0; i < columns.length; i++) {
                    Object value = row.get(i);
                    writer.print(value != null ? value.toString() : "");
                    if (i < columns.length - 1) {
                        writer.print(",");
                    }
                }
                writer.println();
            }
            
            writer.flush();
            return baos.toByteArray();
        }
    }
}
