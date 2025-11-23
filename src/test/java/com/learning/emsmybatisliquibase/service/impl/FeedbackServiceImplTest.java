package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.FeedbackDao;
import com.learning.emsmybatisliquibase.dto.EmployeeDetailsDto;
import com.learning.emsmybatisliquibase.dto.FeedbackResponseDto;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.Feedback;
import com.learning.emsmybatisliquibase.entity.enums.FeedbackType;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.EmployeeMapper;
import com.learning.emsmybatisliquibase.mapper.FeedbackMapper;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @InjectMocks
    FeedbackServiceImpl feedbackService;

    @Mock
    FeedbackDao feedbackDao;

    @Mock
    EmployeeService employeeService;

    @Mock
    EmployeeMapper employeeMapper;

    @Mock
    FeedbackMapper feedbackMapper;

    @Test
    void getById_success() {
        var uuid = UUID.randomUUID();
        var feedback = new Feedback();
        feedback.setUuid(uuid);

        when(feedbackDao.getById(uuid)).thenReturn(feedback);

        var result = feedbackService.getById(uuid);

        assertEquals(feedback, result);
        verify(feedbackDao, times(1)).getById(uuid);
    }

    @Test
    void getById_notFound() {
        when(feedbackDao.getById(any(UUID.class))).thenReturn(null);

        assertThrows(NotFoundException.class, () ->
                feedbackService.getById(UUID.randomUUID()));
    }

    @Test
    void testGetFeedBack() {
        var employeeUuid = UUID.randomUUID();
        var receiverEmployeeUuid = UUID.randomUUID();
        var sender = new Employee();
        sender.setUuid(employeeUuid);
        var receiver = new Employee();
        receiver.setUuid(receiverEmployeeUuid);
        var senderDetails = new EmployeeDetailsDto();
        senderDetails.setUuid(employeeUuid);
        var receiverDetails = new EmployeeDetailsDto();
        receiverDetails.setUuid(receiverEmployeeUuid);
        var type = FeedbackType.SEND;
        var feedbackList = List.of(
                new Feedback(UUID.randomUUID(), employeeUuid, receiverEmployeeUuid, type, "", "", "", LocalDateTime.now(), LocalDateTime.now())
        );
        var feedbackResponse = new FeedbackResponseDto();
        feedbackResponse.setUuid(feedbackList.get(0).getUuid());

        when(feedbackDao.findSendFeedback(employeeUuid, type)).thenReturn(feedbackList);
        when(employeeService.getById(employeeUuid)).thenReturn(sender);
        when(employeeMapper.employeeToEmployeeDetailsDto(sender)).thenReturn(senderDetails);
        when(employeeService.getById(receiverEmployeeUuid)).thenReturn(receiver);
        when(employeeMapper.employeeToEmployeeDetailsDto(receiver)).thenReturn(receiverDetails);
        when(feedbackMapper.feedbackToFeedbackResponseDto(feedbackList.get(0))).thenReturn(feedbackResponse);

        feedbackService.getFeedback(employeeUuid, type);

        verify(feedbackDao, times(1)).findSendFeedback(employeeUuid, type);
        verify(employeeService, times(1)).getById(employeeUuid);
        verify(employeeMapper, times(1)).employeeToEmployeeDetailsDto(sender);
        verify(employeeService, times(1)).getById(receiverEmployeeUuid);
        verify(employeeMapper, times(1)).employeeToEmployeeDetailsDto(receiver);
        verify(feedbackMapper, times(1)).feedbackToFeedbackResponseDto(feedbackList.get(0));
    }

    @Test
    void testSend() {
        var feedback = new Feedback();
        feedback.setUuid(UUID.randomUUID());
        when(feedbackDao.insert(feedback)).thenReturn(1);

        var result = feedbackService.send(feedback);

        assertEquals(result.getUuid(), feedback.getUuid());

        verify(feedbackDao, times(1)).insert(feedback);
    }

    @Test
    void testSend_except() {
        when(feedbackDao.insert(any(Feedback.class))).thenReturn(0);

        assertThrows(IntegrityException.class, () ->
                feedbackService.send(new Feedback()));
    }

    @Test
    void testUpdate() {
        var feedbackId = UUID.randomUUID();
        var feedback = new Feedback();
        feedback.setUuid(feedbackId);

        when(feedbackDao.getById(feedbackId)).thenReturn(feedback);
        when(feedbackDao.update(feedback)).thenReturn(1);

        var response = feedbackService.update(feedbackId, feedback);

        assertEquals(feedback, response);

        verify(feedbackDao, times(1)).getById(feedbackId);
        verify(feedbackDao, times(1)).update(feedback);
    }

    @Test
    void testUpdate_except() {
        var feedbackId = UUID.randomUUID();
        var feedback = new Feedback();
        feedback.setUuid(feedbackId);
        when(feedbackDao.getById(feedbackId)).thenReturn(feedback);
        when(feedbackDao.update(feedback)).thenReturn(0);

        assertThrows(IntegrityException.class, () ->
                feedbackService.update(feedbackId, feedback));

        verify(feedbackDao, times(1)).getById(feedbackId);
        verify(feedbackDao, times(1)).update(feedback);
    }

    @Test
    void testDelete() {
        var uuid = UUID.randomUUID();
        var feedback = new Feedback();
        feedback.setUuid(uuid);
        when(feedbackDao.getById(uuid)).thenReturn(feedback);
        when(feedbackDao.delete(uuid)).thenReturn(1);

        feedbackService.delete(uuid);

        verify(feedbackDao, times(1)).getById(uuid);
        verify(feedbackDao, times(1)).delete(uuid);
    }

    @Test
    void testDelete_except() {
        var uuid = UUID.randomUUID();
        var feedback = new Feedback();
        feedback.setUuid(uuid);
        when(feedbackDao.getById(uuid)).thenReturn(feedback);
        when(feedbackDao.delete(uuid)).thenReturn(0);

        assertThrows(IntegrityException.class, () ->
                feedbackService.delete(uuid));

        verify(feedbackDao, times(1)).getById(uuid);
        verify(feedbackDao, times(1)).delete(uuid);
    }
}