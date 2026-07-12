package com.hackhathon.controller;

import com.hackhathon.entity.Maintenance;
import com.hackhathon.entity.Vehicle;
import com.hackhathon.repository.MaintenanceRepository;
import com.hackhathon.repository.VehicleRepository;
import com.hackhathon.service.AuditLogService;
import com.hackhathon.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public MaintenanceController(MaintenanceRepository maintenanceRepository, 
                                 VehicleRepository vehicleRepository,
                                 AuditLogService auditLogService,
                                 NotificationService notificationService) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listMaintenance(Model model) {
        List<Maintenance> logs = maintenanceRepository.findAll();
        model.addAttribute("maintenanceLogs", logs);
        return "maintenance/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("maintenance", new Maintenance());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "maintenance/form";
    }

    @PostMapping("/new")
    public String createMaintenance(@Valid @ModelAttribute("maintenance") Maintenance maintenance,
                                     BindingResult result,
                                     @RequestParam("vehicleId") Long vehicleId,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicleRepository.findAll());
            return "maintenance/form";
        }

        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        if (vOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected vehicle not found");
            return "redirect:/maintenance/new";
        }

        Vehicle vehicle = vOpt.get();
        maintenance.setVehicle(vehicle);
        maintenance.setStartDate(LocalDateTime.now());
        
        maintenanceRepository.save(maintenance);

        // If in progress, mark vehicle IN_SHOP
        if ("IN_PROGRESS".equalsIgnoreCase(maintenance.getStatus())) {
            vehicle.setStatus("IN_SHOP");
            vehicleRepository.save(vehicle);
        }

        auditLogService.log("MAINTENANCE_CREATE", "Scheduled maintenance for vehicle " + vehicle.getVehicleName() + ": " + maintenance.getDescription());
        notificationService.createNotification("Maintenance scheduled for " + vehicle.getVehicleName(), "ADMIN");
        notificationService.createNotification("Maintenance scheduled for " + vehicle.getVehicleName(), "FLEET_MANAGER");

        redirectAttributes.addFlashAttribute("successMessage", "Maintenance scheduled successfully.");
        return "redirect:/maintenance";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Maintenance> opt = maintenanceRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Maintenance log not found");
            return "redirect:/maintenance";
        }
        model.addAttribute("maintenance", opt.get());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "maintenance/form";
    }

    @PostMapping("/edit/{id}")
    public String updateMaintenance(@PathVariable("id") Long id,
                                     @Valid @ModelAttribute("maintenance") Maintenance maintenance,
                                     BindingResult result,
                                     @RequestParam("vehicleId") Long vehicleId,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicleRepository.findAll());
            return "maintenance/form";
        }
        
        Optional<Maintenance> existingOpt = maintenanceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Maintenance log not found");
            return "redirect:/maintenance";
        }

        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        if (vOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vehicle not found");
            return "redirect:/maintenance";
        }

        Maintenance existing = existingOpt.get();
        Vehicle vehicle = vOpt.get();

        existing.setVehicle(vehicle);
        existing.setDescription(maintenance.getDescription());
        existing.setCost(maintenance.getCost());
        existing.setStatus(maintenance.getStatus());
        
        // Handle transitions
        if ("COMPLETED".equalsIgnoreCase(maintenance.getStatus())) {
            existing.setCompletionDate(LocalDateTime.now());
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        } else if ("IN_PROGRESS".equalsIgnoreCase(maintenance.getStatus())) {
            vehicle.setStatus("IN_SHOP");
            vehicleRepository.save(vehicle);
        }

        maintenanceRepository.save(existing);
        auditLogService.log("MAINTENANCE_UPDATE", "Updated maintenance for vehicle " + vehicle.getVehicleName() + " (ID: " + id + ")");
        notificationService.createNotification("Maintenance updated: " + vehicle.getVehicleName(), "ADMIN");
        notificationService.createNotification("Maintenance updated: " + vehicle.getVehicleName(), "FLEET_MANAGER");
        redirectAttributes.addFlashAttribute("successMessage", "Maintenance log updated successfully.");
        return "redirect:/maintenance";
    }

    @GetMapping("/complete/{id}")
    public String completeMaintenance(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<Maintenance> opt = maintenanceRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Maintenance log not found");
            return "redirect:/maintenance";
        }

        Maintenance maintenance = opt.get();
        maintenance.setStatus("COMPLETED");
        maintenance.setCompletionDate(LocalDateTime.now());
        maintenanceRepository.save(maintenance);

        Vehicle vehicle = maintenance.getVehicle();
        if (vehicle != null) {
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        }

        auditLogService.log("MAINTENANCE_COMPLETE", "Completed maintenance " + id + " for vehicle " + (vehicle != null ? vehicle.getVehicleName() : "N/A"));
        notificationService.createNotification("Maintenance completed for " + (vehicle != null ? vehicle.getVehicleName() : ""), "ADMIN");
        notificationService.createNotification("Maintenance completed for " + (vehicle != null ? vehicle.getVehicleName() : ""), "FLEET_MANAGER");

        redirectAttributes.addFlashAttribute("successMessage", "Maintenance completed. Vehicle is now back in service.");
        return "redirect:/maintenance";
    }

    @GetMapping("/delete/{id}")
    public String deleteMaintenance(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        maintenanceRepository.deleteById(id);
        auditLogService.log("MAINTENANCE_DELETE", "Deleted maintenance log " + id);
        redirectAttributes.addFlashAttribute("successMessage", "Maintenance log deleted!");
        return "redirect:/maintenance";
    }
}
