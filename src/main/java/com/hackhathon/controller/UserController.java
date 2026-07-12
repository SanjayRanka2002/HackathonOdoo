package com.hackhathon.controller;

import com.hackhathon.entity.Notification;
import com.hackhathon.entity.User;
import com.hackhathon.repository.NotificationRepository;
import com.hackhathon.repository.UserRepository;
import com.hackhathon.service.AuditLogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          NotificationRepository notificationRepository,
                          AuditLogService auditLogService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    // --- PROFILE / SETTINGS ---

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Active user profile details not found");
            return "redirect:/";
        }
        model.addAttribute("user", userOpt.get());
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam("firstName") String firstName,
                                 @RequestParam("lastName") String lastName,
                                 @RequestParam("email") String email,
                                 @RequestParam("phone") String phone,
                                 @RequestParam(value = "newPassword", required = false) String newPassword,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User profile not found");
            return "redirect:/profile";
        }

        User user = userOpt.get();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);

        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        auditLogService.log("PROFILE_UPDATE", "Updated personal profile settings");
        redirectAttributes.addFlashAttribute("successMessage", "Profile credentials updated successfully!");
        return "redirect:/profile";
    }

    // --- NOTIFICATIONS VIEW & MANAGEMENT ---

    @GetMapping("/notifications")
    public String viewNotifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isPresent()) {
            List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userOpt.get().getId());
            model.addAttribute("notifications", list);
        }
        return "notifications/list";
    }

    @GetMapping("/notifications/read/{id}")
    public String markAsRead(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        Optional<Notification> nOpt = notificationRepository.findById(id);
        if (nOpt.isPresent()) {
            Notification notification = nOpt.get();
            if (userDetails != null && notification.getUser().getUsername().equals(userDetails.getUsername())) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
        }
        return "redirect:/notifications";
    }

    @GetMapping("/notifications/clear")
    public String clearAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isPresent()) {
            List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userOpt.get().getId());
            notificationRepository.deleteAll(list);
        }
        return "redirect:/notifications";
    }
}
