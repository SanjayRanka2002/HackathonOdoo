package com.hackhathon.controller;

import com.hackhathon.entity.Driver;
import com.hackhathon.entity.DriverDocument;
import com.hackhathon.repository.DriverDocumentRepository;
import com.hackhathon.repository.DriverRepository;
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
@RequestMapping("/drivers")
public class DriverController {

    private final DriverRepository driverRepository;
    private final DriverDocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    private static final String UPLOAD_DIR = "uploads/";

    public DriverController(DriverRepository driverRepository, 
                            DriverDocumentRepository documentRepository,
                            AuditLogService auditLogService,
                            NotificationService notificationService) {
        this.driverRepository = driverRepository;
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listDrivers(Model model) {
        List<Driver> drivers = driverRepository.findAll();
        model.addAttribute("drivers", drivers);
        return "drivers/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("driver", new Driver());
        return "drivers/form";
    }

    @PostMapping("/new")
    public String createDriver(@Valid @ModelAttribute("driver") Driver driver,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "drivers/form";
        }
        if (driverRepository.findByLicenseNumber(driver.getLicenseNumber()).isPresent()) {
            result.rejectValue("licenseNumber", "error.driver", "License number already exists");
            return "drivers/form";
        }
        driverRepository.save(driver);
        auditLogService.log("DRIVER_CREATE", "Enlisted driver " + driver.getDriverName() + " with license " + driver.getLicenseNumber());
        notificationService.createNotification("New driver profile created: " + driver.getDriverName(), "ADMIN");
        notificationService.createNotification("New driver profile created: " + driver.getDriverName(), "FLEET_MANAGER");
        redirectAttributes.addFlashAttribute("successMessage", "Driver created successfully!");
        return "redirect:/drivers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Driver> opt = driverRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Driver not found");
            return "redirect:/drivers";
        }
        model.addAttribute("driver", opt.get());
        return "drivers/form";
    }

    @PostMapping("/edit/{id}")
    public String updateDriver(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("driver") Driver driver,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "drivers/form";
        }
        Optional<Driver> existingOpt = driverRepository.findById(id);
        if (existingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Driver not found");
            return "redirect:/drivers";
        }

        Driver existing = existingOpt.get();
        if (!existing.getLicenseNumber().equalsIgnoreCase(driver.getLicenseNumber())) {
            if (driverRepository.findByLicenseNumber(driver.getLicenseNumber()).isPresent()) {
                result.rejectValue("licenseNumber", "error.driver", "License number already exists");
                return "drivers/form";
            }
        }

        existing.setDriverName(driver.getDriverName());
        existing.setLicenseNumber(driver.getLicenseNumber());
        existing.setLicenseCategory(driver.getLicenseCategory());
        existing.setExpiryDate(driver.getExpiryDate());
        existing.setPhone(driver.getPhone());
        existing.setEmail(driver.getEmail());
        existing.setAddress(driver.getAddress());
        existing.setEmergencyContact(driver.getEmergencyContact());
        existing.setSafetyScore(driver.getSafetyScore());
        existing.setStatus(driver.getStatus());

        driverRepository.save(existing);
        auditLogService.log("DRIVER_UPDATE", "Updated driver " + existing.getDriverName() + " (ID: " + id + ")");
        notificationService.createNotification("Driver profile updated: " + existing.getDriverName(), "ADMIN");
        notificationService.createNotification("Driver profile updated: " + existing.getDriverName(), "FLEET_MANAGER");
        redirectAttributes.addFlashAttribute("successMessage", "Driver updated successfully!");
        return "redirect:/drivers";
    }

    @GetMapping("/{id}")
    public String viewDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Driver> opt = driverRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Driver not found");
            return "redirect:/drivers";
        }
        model.addAttribute("driver", opt.get());
        model.addAttribute("documents", documentRepository.findByDriverId(id));
        return "drivers/details";
    }

    @GetMapping("/delete/{id}")
    public String deleteDriver(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Driver> opt = driverRepository.findById(id);
            String name = opt.isPresent() ? opt.get().getDriverName() : ("ID:" + id);
            driverRepository.deleteById(id);
            auditLogService.log("DRIVER_DELETE", "Deleted driver " + name);
            notificationService.createNotification("Driver profile deleted: " + name, "ADMIN");
            notificationService.createNotification("Driver profile deleted: " + name, "FLEET_MANAGER");
            redirectAttributes.addFlashAttribute("successMessage", "Driver deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete driver. Ensure they are not currently assigned to a trip.");
        }
        return "redirect:/drivers";
    }

    @PostMapping("/{id}/documents")
    public String uploadDocument(@PathVariable("id") Long id,
                                 @RequestParam("documentName") String documentName,
                                 @RequestParam("documentType") String documentType,
                                 @RequestParam("expiryDate") String expiryDateStr,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        Optional<Driver> driverOpt = driverRepository.findById(id);
        if (driverOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Driver not found");
            return "redirect:/drivers";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            return "redirect:/drivers/" + id;
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            DriverDocument desc = DriverDocument.builder()
                    .documentName(documentName)
                    .documentType(documentType)
                    .filePath("/uploads/" + fileName)
                    .expiryDate(expiryDateStr.isBlank() ? null : LocalDate.parse(expiryDateStr))
                    .driver(driverOpt.get())
                    .build();

            documentRepository.save(desc);
            auditLogService.log("DRIVER_DOCUMENT_UPLOAD", "Uploaded document " + documentName + " for driver " + driverOpt.get().getDriverName());
            redirectAttributes.addFlashAttribute("successMessage", "Document uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload document: " + e.getMessage());
        }

        return "redirect:/drivers/" + id;
    }
}
