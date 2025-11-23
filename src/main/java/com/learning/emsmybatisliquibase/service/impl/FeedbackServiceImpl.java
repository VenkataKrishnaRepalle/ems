package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.FeedbackDao;
import com.learning.emsmybatisliquibase.dto.FeedbackResponseDto;
import com.learning.emsmybatisliquibase.dto.RequestFeedbackDto;
import com.learning.emsmybatisliquibase.entity.Feedback;
import com.learning.emsmybatisliquibase.entity.enums.FeedbackType;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.EmployeeMapper;
import com.learning.emsmybatisliquibase.mapper.FeedbackMapper;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackDao feedbackDao;

    private final EmployeeService employeeService;

    private final FeedbackMapper feedbackMapper;

    private final EmployeeMapper employeeMapper;

    @Override
    public Feedback getById(UUID uuid) {
        var feedback = feedbackDao.getById(uuid);
        if (feedback == null) {
            throw new NotFoundException("FEEDBACK_NOT_FOUND", "Feedback not found with uuid: " + uuid);
        }
        return feedback;
    }

    @Override
    public List<FeedbackResponseDto> getFeedback(UUID employeeUuid, FeedbackType feedbackType) {
        List<Feedback> feedbacks;
        if (feedbackType.equals(FeedbackType.SEND) || feedbackType.equals(FeedbackType.DRAFT)) {
            feedbacks = feedbackDao.findSendFeedback(employeeUuid, feedbackType);
        } else {
            feedbacks = feedbackDao.findReceivedFeedback(employeeUuid, feedbackType);
        }
        assert feedbacks != null;
        return feedbacks.stream()
                .map(this::toFeedbackResponse)
                .toList();
    }

    private FeedbackResponseDto toFeedbackResponse(Feedback feedback) {
        var sender = employeeMapper.employeeToEmployeeDetailsDto(
                employeeService.getById(feedback.getEmployeeUuid()));
        var receiver = employeeMapper.employeeToEmployeeDetailsDto(
                employeeService.getById(feedback.getTargetEmployeeUuid()));
        var feedbackResponse = feedbackMapper.feedbackToFeedbackResponseDto(feedback);
        feedbackResponse.setSender(sender);
        feedbackResponse.setReceiver(receiver);
        return feedbackResponse;
    }

    @Override
    public Feedback send(Feedback feedback) {
        feedback.setUuid(UUID.randomUUID());
        feedback.setCreatedTime(LocalDateTime.now());
        feedback.setUpdatedTime(LocalDateTime.now());

        try {
            if (0 == feedbackDao.insert(feedback)) {
                throw new IntegrityException("FEEDBACK_NOT_CREATED", "feedback not created");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("FEEDBACK_NOT_CREATED", exception.getCause().getMessage());
        }

        return feedback;
    }

    @Override
    public Feedback update(UUID feedbackId, Feedback feedback) {
        getById(feedback.getUuid());

        try {
            if (0 == feedbackDao.update(feedback)) {
                throw new IntegrityException("FEEDBACK_NOT_UPDATED",
                        "Feedback not updated with uuid: " + feedback.getUuid());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("FEEDBACK_NOT_UPDATED", exception.getCause().getMessage());
        }

        return feedback;
    }

    @Override
    public void delete(UUID feedbackId) {
        getById(feedbackId);

        try {
            if (0 == feedbackDao.delete(feedbackId)) {
                throw new IntegrityException("FEEDBACK_NOT_DELETED",
                        "Feedback not deleted with uuid: " + feedbackId);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("FEEDBACK_NOT_DELETED", exception.getCause().getMessage());
        }
    }

    @Override
    public Feedback requestFeedback(RequestFeedbackDto requestFeedbackDto) {
        var feedback = feedbackMapper.requestFeedbackDtoToFeedback(requestFeedbackDto);
        feedback.setType(FeedbackType.REQUEST);
        /* Save Feedback*/
        return send(feedback);
    }

    @Override
    public List<FeedbackResponseDto> getAll(UUID employeeUuid) {
        var feedbacks = feedbackDao.getAll(employeeUuid);
        return feedbacks.stream()
                .map(this::toFeedbackResponse)
                .toList();
    }
}
