package com.hackhathon.controller;

import com.hackhathon.entity.AuditLog;
import com.hackhathon.entity.Role;
import com.hackhathon.entity.User;
import com.hackhathon.repository.AuditLogRepository;
import com.hackhathon.repository.RoleRepository;
import com.hackhathon.repository.UserRepository;
import com.hackhathon.service.AuditLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           AuditLogRepository auditLogRepository,
                           AuditLogService auditLogService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    // --- USERS MANAGEMENT ---

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/user_form";
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             @RequestParam("roleId") Long roleId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/user_form";
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            result.rejectValue("username", "error.user", "Username already exists");
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/user_form";
        }

        Optional<Role> rOpt = roleRepository.findById(roleId);
        if (rOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected role not found");
            return "redirect:/admin/users/new";
        }

        user.setRole(rOpt.get());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        auditLogService.log("USER_CREATE", "Created user account: " + user.getUsername() + " with role: " + rOpt.get().getName());
        redirectAttributes.addFlashAttribute("successMessage", "User account created successfully!");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", opt.get());
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/user_form";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             @RequestParam("roleId") Long roleId,
                             @RequestParam(value = "newPassword", required = false) String newPassword,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/user_form";
        }

        Optional<User> existingOpt = userRepository.findById(id);
        if (existingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/admin/users";
        }

        User existing = existingOpt.get();

        if (!existing.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                result.rejectValue("username", "error.user", "Username already exists");
                model.addAttribute("roles", roleRepository.findAll());
                return "admin/user_form";
            }
        }

        Optional<Role> rOpt = roleRepository.findById(roleId);
        if (rOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Role not found");
            return "redirect:/admin/users";
        }

        existing.setUsername(user.getUsername());
        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setActive(user.getActive());
        existing.setRole(rOpt.get());

        if (newPassword != null && !newPassword.isBlank()) {
            existing.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(existing);
        auditLogService.log("USER_UPDATE", "Updated user details for account: " + existing.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/admin/users";
        }
        
        User user = opt.get();
        if ("admin".equals(user.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Root admin account cannot be deleted!");
            return "redirect:/admin/users";
        }

        userRepository.deleteById(id);
        auditLogService.log("USER_DELETE", "Deleted user account: " + user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "User account removed!");
        return "redirect:/admin/users";
    }

    // --- SYSTEM AUDIT LOGS ---

    @GetMapping("/audit-logs")
    public String listAuditLogs(Model model) {
        List<AuditLog> logs = auditLogRepository.findAll();
        // Sort newest first
        logs.sort((l1, l2) -> l2.getTimestamp().compareTo(l1.getTimestamp()));
        model.addAttribute("auditLogs", logs);
        return "admin/audit_logs";
    }
}
