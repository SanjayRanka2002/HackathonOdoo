package com.hackhathon.controller;

import com.hackhathon.entity.Notification;
import com.hackhathon.entity.User;
import com.hackhathon.repository.NotificationRepository;
import com.hackhathon.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public GlobalControllerAdvice(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @ModelAttribute("unreadNotificationsCount")
    public long getUnreadNotificationsCount() {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isPresent()) {
            return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.get().getId()).stream()
                    .filter(n -> !n.getIsRead())
                    .count();
        }
        return 0;
    }

    @ModelAttribute("recentNotifications")
    public List<Notification> getRecentNotifications() {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isPresent()) {
            List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.get().getId());
            return list.size() > 5 ? list.subList(0, 5) : list;
        }
        return new ArrayList<>();
    }

    @ModelAttribute("currentUserRole")
    public String getCurrentUserRole() {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isPresent() && currentUser.get().getRole() != null) {
            return currentUser.get().getRole().getName();
        }
        return "GUEST";
    }

    private Optional<User> getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return Optional.empty();
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username);
        }
        return Optional.empty();
    }
}
