package com.learning.emsmybatisliquibase.Kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dto.EmployeeEtmsDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeDetailsEtmsProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public void sendNewEmployee(EmployeeEtmsDetails employee) {
        try {
            kafkaTemplate.send("send-new-employee-details-to-etms", employee.getUuid().toString(),
                    objectMapper.writeValueAsString(employee));
            log.info("New employee details sent to etms {}", employee.getName());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendUpdateEmployee(EmployeeEtmsDetails employee) {
        try {
            kafkaTemplate.send("send-update-employee-details-to-etms", employee.getUuid().toString(),
                    objectMapper.writeValueAsString(employee));
            log.info("Updated employee details sent to etms {}", employee.getName());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
