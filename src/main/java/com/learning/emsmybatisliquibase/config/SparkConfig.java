package com.learning.emsmybatisliquibase.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class SparkConfig {

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession() {
        SparkSession spark = SparkSession.builder()
                .appName("spring-boot-spark-starter")
                .master("local[*]")
                .config("spark.driver.bindAddress", "0.0.0.0")
                .config("spark.nio.buffer.max", "268435456")
                .config("spark.ui.enabled", "false")
                .getOrCreate();

        String accountName = "devstoreaccount1";
        String accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

        spark.sparkContext().hadoopConfiguration().set(String.format("fs.azure.account.key.%s.blob.core.windows.net", accountName), accountKey);
        spark.sparkContext().hadoopConfiguration().set("fs.azure.always.use.https", "false");
        spark.sparkContext().hadoopConfiguration().set(String.format("fs.azure.account.blob.endpoint.%s.blob.core.windows.net", accountName), "http://127.0.0.1:10000/" + accountName);

        return spark;
    }

    @Bean(name = "dbProperties")
    public Properties dbProperties() {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("driver", driverClassName);
        return properties;
    }
}
