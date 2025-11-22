package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.entity.Notification;
import com.learning.emsmybatisliquibase.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/get/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable UUID id) {
        return new ResponseEntity<>(notificationService.getById(id), HttpStatus.OK);
    }

    @GetMapping("/get-by-employee/{employeeUuid}/status")
    public ResponseEntity<List<Notification>> getNotificationByEmployee(@PathVariable UUID employeeUuid,
                                                                        @RequestParam(name = "statuses", required = false) List<Notification.Status> statuses,
                                                                        @RequestParam(name = "page", defaultValue = "1") int page) {
        return new ResponseEntity<>(notificationService.getByEmployee(employeeUuid, statuses, page), HttpStatus.OK);
    }

    @PostMapping("/send")
    public ResponseEntity<HttpStatus> sendNotification(@RequestBody Notification notification) {
        notificationService.send(notification);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/update/{id}/status/{status}")
    public ResponseEntity<HttpStatus> updateById(@PathVariable UUID id, @PathVariable Notification.Status status) {
        notificationService.updateById(id, status);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PatchMapping("/updateByEmployee/{employeeId}/status/{status}")
    public ResponseEntity<HttpStatus> updateByEmployee(@PathVariable UUID employeeId, @PathVariable Notification.Status status) {
        notificationService.updateByEmployee(employeeId, status);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<HttpStatus> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/delete-by-employee/{employeeUuid}")
    public ResponseEntity<HttpStatus> deleteNotificationByEmployee(@PathVariable UUID employeeUuid) {
        notificationService.deleteByEmployee(employeeUuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
