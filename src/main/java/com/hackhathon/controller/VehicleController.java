package com.hackhathon.controller;

import com.hackhathon.entity.Vehicle;
import com.hackhathon.entity.VehicleDocument;
import com.hackhathon.repository.VehicleDocumentRepository;
import com.hackhathon.repository.VehicleRepository;
import com.hackhathon.service.AuditLogService;
import com.hackhathon.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    
    private static final String UPLOAD_DIR = "uploads/";

    public VehicleController(VehicleRepository vehicleRepository, 
                             VehicleDocumentRepository documentRepository,
                             AuditLogService auditLogService,
                             NotificationService notificationService) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listVehicles(Model model) {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        model.addAttribute("vehicles", vehicles);
        return "vehicles/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        return "vehicles/form";
    }

    @PostMapping("/new")
    public String createVehicle(@Valid @ModelAttribute("vehicle") Vehicle vehicle, 
                                BindingResult result, 
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "vehicles/form";
        }
        if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
            result.rejectValue("registrationNumber", "error.vehicle", "Registration number already exists");
            return "vehicles/form";
        }
        vehicleRepository.save(vehicle);
        auditLogService.log("VEHICLE_CREATE", "Created vehicle asset: " + vehicle.getVehicleName() + " (" + vehicle.getRegistrationNumber() + ")");
        notificationService.createNotification("New vehicle registered in fleet: " + vehicle.getVehicleName(), "ADMIN");
        notificationService.createNotification("New vehicle registered in fleet: " + vehicle.getVehicleName(), "FLEET_MANAGER");
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle created successfully!");
        return "redirect:/vehicles";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vehicle not found");
            return "redirect:/vehicles";
        }
        model.addAttribute("vehicle", opt.get());
        return "vehicles/form";
    }

    @PostMapping("/edit/{id}")
    public String updateVehicle(@PathVariable("id") Long id, 
                                @Valid @ModelAttribute("vehicle") Vehicle vehicle, 
                                BindingResult result, 
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "vehicles/form";
        }
        Optional<Vehicle> existingOpt = vehicleRepository.findById(id);
        if (existingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vehicle not found");
            return "redirect:/vehicles";
        }
        
        Vehicle existing = existingOpt.get();
        // check duplicate registration status if changed
        if (!existing.getRegistrationNumber().equalsIgnoreCase(vehicle.getRegistrationNumber())) {
            if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
                result.rejectValue("registrationNumber", "error.vehicle", "Registration number already exists");
                return "vehicles/form";
            }
        }

        existing.setRegistrationNumber(vehicle.getRegistrationNumber());
        existing.setVehicleName(vehicle.getVehicleName());
        existing.setVehicleType(vehicle.getVehicleType());
        existing.setVehicleModel(vehicle.getVehicleModel());
        existing.setLoadCapacity(vehicle.getLoadCapacity());
        existing.setPurchaseCost(vehicle.getPurchaseCost());
        existing.setCurrentOdometer(vehicle.getCurrentOdometer());
        existing.setStatus(vehicle.getStatus());

        vehicleRepository.save(existing);
        auditLogService.log("VEHICLE_UPDATE", "Updated vehicle asset: " + existing.getVehicleName() + " (ID: " + id + ")");
        notificationService.createNotification("Vehicle specs updated: " + existing.getVehicleName(), "ADMIN");
        notificationService.createNotification("Vehicle specs updated: " + existing.getVehicleName(), "FLEET_MANAGER");
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle updated successfully!");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}")
    public String viewDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vehicle not found");
            return "redirect:/vehicles";
        }
        model.addAttribute("vehicle", opt.get());
        model.addAttribute("documents", documentRepository.findByVehicleId(id)); // findByVehicleId is expected in repository
        return "vehicles/details";
    }

    @GetMapping("/delete/{id}")
    public String deleteVehicle(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Vehicle> opt = vehicleRepository.findById(id);
            String name = opt.isPresent() ? opt.get().getVehicleName() : ("ID:" + id);
            vehicleRepository.deleteById(id);
            auditLogService.log("VEHICLE_DELETE", "Retired vehicle asset: " + name);
            notificationService.createNotification("Vehicle retired from fleet: " + name, "ADMIN");
            notificationService.createNotification("Vehicle retired from fleet: " + name, "FLEET_MANAGER");
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete vehicle. Ensure it has no active assignments.");
        }
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/documents")
    public String uploadDocument(@PathVariable("id") Long id,
                                 @RequestParam("documentName") String documentName,
                                 @RequestParam("documentType") String documentType,
                                 @RequestParam("expiryDate") String expiryDateStr,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(id);
        if (vehicleOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vehicle not found");
            return "redirect:/vehicles";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            return "redirect:/vehicles/" + id;
        }

        try {
            // Check upload dir
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            VehicleDocument desc = VehicleDocument.builder()
                    .documentName(documentName)
                    .documentType(documentType)
                    .filePath("/uploads/" + fileName)
                    .expiryDate(expiryDateStr.isBlank() ? null : LocalDate.parse(expiryDateStr))
                    .vehicle(vehicleOpt.get())
                    .build();

            documentRepository.save(desc);
            auditLogService.log("VEHICLE_DOCUMENT_UPLOAD", "Uploaded document " + documentName + " for vehicle " + vehicleOpt.get().getVehicleName());
            redirectAttributes.addFlashAttribute("successMessage", "Document uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload document: " + e.getMessage());
        }

        return "redirect:/vehicles/" + id;
    }
}
