package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Feedback {

    private UUID uuid;

    private UUID employeeUuid;

    private UUID targetEmployeeUuid;

    private FeedbackType type;

    private String lookBack;

    private String lookForward;

    private String otherComments;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
