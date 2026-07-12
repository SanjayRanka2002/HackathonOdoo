package com.hackhathon.service;

import com.hackhathon.entity.Notification;
import com.hackhathon.entity.User;
import com.hackhathon.repository.NotificationRepository;
import com.hackhathon.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void createNotification(String message, String targetRoleName) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().getName().equalsIgnoreCase(targetRoleName))
                .toList();
        for (User user : users) {
            createNotificationForUser(message, user);
        }
    }

    public void createNotificationForUser(String message, User user) {
        Notification notification = Notification.builder()
                .message(message)
                .isRead(false)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }
    
    public void createNotificationForAdmin(String message) {
        createNotification(message, "ADMIN");
    }

    public void createGlobalNotification(String message) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            createNotificationForUser(message, user);
        }
    }
}
