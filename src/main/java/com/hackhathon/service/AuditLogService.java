package com.hackhathon.service;

import com.hackhathon.entity.AuditLog;
import com.hackhathon.repository.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String details) {
        String username = "SYSTEM";
        Object principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal != null) {
            Object principalObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principalObj instanceof UserDetails) {
                username = ((UserDetails) principalObj).getUsername();
            } else if (principalObj != null) {
                username = principalObj.toString();
            }
        }
        
        AuditLog log = AuditLog.builder()
                .action(action)
                .username(username)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
