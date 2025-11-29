package com.learning.emsmybatisliquibase.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SparkConfig {


    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("spring-boot-spark-starter")
                .master("local[*]")
                .config("spark.driver.bindAddress", "0.0.0.0")
                .config("spark.nio.buffer.max", "268435456")
                .config("spark.ui.enabled", "false")
                .getOrCreate();
    }
}
