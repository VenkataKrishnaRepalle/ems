FROM eclipse-temurin:21-jre-alpine
LABEL authors="rvenkata"

# Copy the application jar file
ARG JAR_FILE=target/ems-mybatis-liquibase.jar
COPY ${JAR_FILE} ems-mybatis-liquibase.jar

# Expose the application port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", "-jar", "/ems-mybatis-liquibase.jar"]